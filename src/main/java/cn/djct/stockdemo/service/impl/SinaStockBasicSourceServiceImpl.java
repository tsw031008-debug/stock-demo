package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.service.StockBasicSourceService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * 新浪沪深京A股基础信息数据源。
 */
@Slf4j
@Service
public class SinaStockBasicSourceServiceImpl implements StockBasicSourceService {

    private static final int MAX_ATTEMPTS = 2;

    private final RestTemplate restTemplate;

    private final String countUrl;

    private final String listUrl;

    private final int pageSize;

    private final RequestRateLimiter requestRateLimiter;

    @Autowired
    public SinaStockBasicSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.basic.source.count-url}") String countUrl,
            @Value("${stock.basic.source.list-url}") String listUrl,
            @Value("${stock.basic.source.page-size:100}") int pageSize,
            @Value("${stock.basic.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.basic.source.read-timeout:10s}") Duration readTimeout,
            @Value("${stock.basic.source.request-interval:1s}") Duration requestInterval
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(connectTimeout)
                        .setReadTimeout(readTimeout)
                        .additionalMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                        .build(),
                countUrl,
                listUrl,
                pageSize,
                requestInterval
        );
    }

    // 私有构造函数，用于测试
    SinaStockBasicSourceServiceImpl(
            RestTemplate restTemplate,
            String countUrl,
            String listUrl,
            int pageSize,
            Duration requestInterval
    ) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("每页数量必须大于0");
        }
        this.restTemplate = restTemplate;
        this.countUrl = countUrl;
        this.listUrl = listUrl;
        this.pageSize = pageSize;
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    // 获取所有股票基础信息
    @Override
    public List<StockBasicDto> fetchAll() {
        int expectedCount = fetchStockCount();
        int pageCount = (expectedCount + pageSize - 1) / pageSize;
        List<StockBasicDto> stocks = new ArrayList<>(expectedCount);
        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            List<StockBasicDto> pageStocks = fetchStockList(pageNumber);
            int expectedPageCount = Math.min(pageSize, expectedCount - stocks.size());
            if (pageStocks.size() != expectedPageCount) {
                throw new IllegalStateException(
                        "新浪股票清单分页数量不一致，page=" + pageNumber
                                + "，expected=" + expectedPageCount
                                + "，actual=" + pageStocks.size()
                );
            }
            stocks.addAll(pageStocks);
        }
        if (stocks.size() != expectedCount) {
            throw new IllegalStateException(
                    "新浪股票清单数量不一致，expected=" + expectedCount + "，actual=" + stocks.size()
            );
        }

        log.info("新浪沪深京A股清单获取完成，requestCount={}，stockCount={}", pageCount + 1, stocks.size());
        return stocks;
    }

    // 获取股票总数
    private int fetchStockCount() {
        URI uri = UriComponentsBuilder.fromHttpUrl(countUrl)
                .queryParam("node", "hs_a")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        String responseBody = request(uri, "股票总数");
        String countText = responseBody == null ? "" : responseBody.strip();
        if (countText.length() >= 2 && countText.startsWith("\"") && countText.endsWith("\"")) {
            countText = countText.substring(1, countText.length() - 1);
        }
        if (!countText.matches("[1-9]\\d*")) {
            throw new IllegalStateException("新浪股票总数响应格式错误");
        }
        return Integer.parseInt(countText);
    }

    // 获取股票列表
    private List<StockBasicDto> fetchStockList(int pageNumber) {
        URI uri = UriComponentsBuilder.fromHttpUrl(listUrl)
                .queryParam("page", pageNumber)
                .queryParam("num", pageSize)
                .queryParam("sort", "symbol")
                .queryParam("asc", 1)
                .queryParam("node", "hs_a")
                .queryParam("symbol", "")
                .queryParam("_s_r_a", "page")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        String responseBody = request(uri, "股票清单第" + pageNumber + "页");
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("新浪股票清单响应为空");
        }

        try {
            JSONArray items = JSON.parseArray(responseBody);
            if (items == null) {
                throw new IllegalStateException("新浪股票清单缺少数组内容，page=" + pageNumber);
            }

            List<StockBasicDto> stocks = new ArrayList<>(items.size());
            for (int index = 0; index < items.size(); index++) {
                JSONObject item = items.getJSONObject(index);
                String stockCode = item.getString("code");
                String stockName = item.getString("name");
                validateStock(stockCode, stockName);
                stocks.add(new StockBasicDto(stockCode, stockName.strip()));
            }
            return stocks;
        } catch (JSONException exception) {
            throw new IllegalStateException("新浪股票清单响应格式错误", exception);
        }
    }

    // 发送HTTP请求
    private String request(URI uri, String requestName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.REFERER, "https://vip.stock.finance.sina.com.cn/mkt/#hs_a");
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            requestRateLimiter.acquire();
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );
                return response.getBody();
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("新浪{}请求失败，attempt={}，reason={}", requestName, attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("新浪" + requestName + "请求失败", lastException);
    }

    // 验证股票代码和名称
    private void validateStock(String stockCode, String stockName) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalStateException("新浪返回了无效股票代码：" + stockCode);
        }
        if (stockName == null || stockName.isBlank()) {
            throw new IllegalStateException("新浪返回了空股票名称，stockCode=" + stockCode);
        }
    }

    // 请求速率限制器
    private static final class RequestRateLimiter {

        private final long intervalNanos;

        private long nextRequestNanos;

        private RequestRateLimiter(Duration interval) {
            if (interval.isNegative()) {
                throw new IllegalArgumentException("请求间隔不能小于0");
            }
            this.intervalNanos = interval.toNanos();
        }

        private synchronized void acquire() {
            long now = System.nanoTime();
            long waitNanos = nextRequestNanos - now;
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("新浪股票清单请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
