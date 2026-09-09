package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.service.stockalert.TechnologyStockPoolSourceService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内部板块接口科技股候选池实现。
 */
@Service
public class TechnologyStockPoolSourceServiceImpl implements TechnologyStockPoolSourceService {

    private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("\\d{6}");

    private final RestTemplate restTemplate;
    private final String sourceUrl;

    /**
     * 创建科技股候选池数据源。
     */
    @Autowired
    public TechnologyStockPoolSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.alert.technology.source.url}") String sourceUrl,
            @Value("${stock.alert.technology.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.alert.technology.source.read-timeout:20s}") Duration readTimeout
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
     * 创建使用指定HTTP客户端的数据源，供固定样本测试使用。
     */
    TechnologyStockPoolSourceServiceImpl(RestTemplate restTemplate, String sourceUrl) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
    }

    /**
     * 请求并解析科技板块股票代码。
     */
    @Override
    public List<String> fetchStockCodes() {
        try {
            JSONObject root = JSON.parseObject(request());
            if (root == null || root.getIntValue("code") != 200) {
                throw new IllegalStateException("科技股候选池响应状态异常");
            }
            JSONArray data = root.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                throw new IllegalStateException("科技股候选池数据为空");
            }

            Set<String> stockCodes = new LinkedHashSet<>(data.size());
            for (Object value : data) {
                String stockCode = value == null ? null : value.toString().trim();
                if (stockCode == null || !STOCK_CODE_PATTERN.matcher(stockCode).matches()) {
                    throw new IllegalStateException("科技股候选池存在非法股票代码");
                }
                stockCodes.add(stockCode);
            }
            return List.copyOf(stockCodes);
        } catch (JSONException exception) {
            throw new IllegalStateException("科技股候选池响应格式错误", exception);
        }
    }

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
                throw new IllegalStateException("科技股候选池响应为空");
            }
            return body;
        } catch (RestClientException exception) {
            throw new IllegalStateException("科技股候选池请求失败", exception);
        }
    }
}
