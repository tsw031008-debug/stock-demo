package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
import cn.djct.stockdemo.service.plate.StockPlateSourceService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 同花顺三级概念板块数据源实现。
 */
@Service
public class ThsStockPlateSourceServiceImpl implements StockPlateSourceService {

    private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("\\d{6}");

    private final RestTemplate restTemplate;
    private final String sourceUrl;

    /**
     * 创建板块数据源服务。
     *
     * @param restTemplateBuilder HTTP客户端构建器
     * @param sourceUrl           板块来源地址
     * @param connectTimeout      连接超时
     * @param readTimeout         读取超时
     */
    @Autowired
    public ThsStockPlateSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.plate.source.url}") String sourceUrl,
            @Value("${stock.plate.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.plate.source.read-timeout:20s}") Duration readTimeout
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(connectTimeout)
                        .setReadTimeout(readTimeout)
                        .build(),
                sourceUrl
        );
    }

    /**
     * 创建使用指定HTTP客户端的数据源服务，供固定样本测试使用。
     *
     * @param restTemplate HTTP客户端
     * @param sourceUrl    板块来源地址
     */
    ThsStockPlateSourceServiceImpl(RestTemplate restTemplate, String sourceUrl) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
    }

    /**
     * 获取并解析完整板块和成分股快照。
     *
     * @return 板块快照列表
     */
    @Override
    public List<StockPlateSourceDto> fetchAll() {
        // 单次请求完整板块快照，避免对来源接口循环访问
        String responseBody = request();
        // 校验顶层业务状态和板块数据对象
        JSONObject root = JSON.parseObject(responseBody);
        if (root == null || root.getIntValue("code") != 200) {
            throw new IllegalStateException("板块来源响应状态异常");
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("板块来源数据为空");
        }

        // 逐板块校验股票代码并在单板块内去重
        List<StockPlateSourceDto> plates = new ArrayList<>(data.size());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String plateName = entry.getKey() == null ? null : entry.getKey().trim();
            if (plateName == null || plateName.isEmpty()) {
                throw new IllegalStateException("板块来源存在空板块名称");
            }
            JSONArray stockArray = JSON.parseArray(JSON.toJSONString(entry.getValue()));
            if (stockArray == null || stockArray.isEmpty()) {
                throw new IllegalStateException("板块没有成分股，plateName=" + plateName);
            }
            Set<String> stockCodes = new LinkedHashSet<>();
            for (Object value : stockArray) {
                String stockCode = value == null ? null : value.toString().trim();
                if (stockCode == null || !STOCK_CODE_PATTERN.matcher(stockCode).matches()) {
                    throw new IllegalStateException("板块股票代码格式错误，plateName=" + plateName);
                }
                stockCodes.add(stockCode);
            }
            plates.add(StockPlateSourceDto.builder()
                    .plateName(plateName)
                    .stockCodes(List.copyOf(stockCodes))
                    .build());
        }
        // 固定板块顺序，便于验证和稳定保存
        plates.sort(Comparator.comparing(StockPlateSourceDto::getPlateName));
        return List.copyOf(plates);
    }

    /**
     * 请求板块来源接口并返回非空响应体。
     */
    private String request() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(sourceUrl),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("板块来源响应为空");
            }
            return body;
        } catch (RestClientException exception) {
            throw new IllegalStateException("板块来源请求失败", exception);
        }
    }
}
