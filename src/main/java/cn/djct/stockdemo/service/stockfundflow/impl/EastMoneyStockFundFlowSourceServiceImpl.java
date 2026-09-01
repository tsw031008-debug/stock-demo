package cn.djct.stockdemo.service.stockfundflow.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSourceService;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

/**
 * 东方财富股票资金流向数据源。
 */
@Slf4j
@Service
public class EastMoneyStockFundFlowSourceServiceImpl implements StockFundFlowSourceService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_ATTEMPTS = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String MARKET_FILTER = "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23";
    private static final String FIELDS = "f12,f14,f2,f3,f62,f184,f66,f69,f72,f75";
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate;
    private final String sourceUrl;
    private final RequestRateLimiter requestRateLimiter;

    @Autowired
    public EastMoneyStockFundFlowSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.fund-flow.source.url}") String sourceUrl,
            @Value("${stock.fund-flow.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.fund-flow.source.read-timeout:15s}") Duration readTimeout,
            @Value("${stock.fund-flow.source.request-interval:2s}") Duration requestInterval
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(connectTimeout)
                        .setReadTimeout(readTimeout)
                        .build(),
                sourceUrl,
                requestInterval
        );
    }

    EastMoneyStockFundFlowSourceServiceImpl(RestTemplate restTemplate, String sourceUrl) {
        this(restTemplate, sourceUrl, Duration.ZERO);
    }

    EastMoneyStockFundFlowSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    @Override
    public List<StockFundFlow> fetchAll(LocalDate tradeDate, List<StockBasic> stocks) {
        if (tradeDate == null) {
            throw new IllegalArgumentException("交易日期不能为空");
        }
        if (stocks == null || stocks.isEmpty()) {
            throw new IllegalArgumentException("待查询股票不能为空");
        }

        Set<String> expectedCodes = new LinkedHashSet<>(stocks.size());
        for (StockBasic stock : stocks) {
            if (!expectedCodes.add(stock.getStockCode())) {
                throw new IllegalStateException("股票清单存在重复代码：" + stock.getStockCode());
            }
        }

        Map<String, JSONObject> sourceItems = fetchAllPages();
        LocalDateTime collectedAt = LocalDateTime.now(SHANGHAI_ZONE);
        List<StockFundFlow> fundFlows = new ArrayList<>(stocks.size());
        for (StockBasic stock : stocks) {
            JSONObject item = sourceItems.get(stock.getStockCode());
            if (item == null) {
                throw new IllegalStateException("东方财富资金流向缺少股票：" + stock.getStockCode());
            }
            fundFlows.add(parseFundFlow(tradeDate, item, collectedAt));
        }

        log.info("东方财富沪深A股资金流向获取完成，sourceCount={}，matchedCount={}",
                sourceItems.size(), fundFlows.size());
        return fundFlows;
    }

    private Map<String, JSONObject> fetchAllPages() {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        Integer expectedTotal = null;
        int pageNumber = 1;
        while (expectedTotal == null || result.size() < expectedTotal) {
            FundFlowPage page = parseResponse(request(buildUri(pageNumber)), pageNumber);
            if (expectedTotal == null) {
                expectedTotal = page.total();
            } else if (expectedTotal != page.total()) {
                throw new IllegalStateException("东方财富资金流向分页总数不一致，page="
                        + pageNumber + "，expected=" + expectedTotal + "，actual=" + page.total());
            }

            int expectedPageSize = Math.min(PAGE_SIZE, expectedTotal - result.size());
            if (page.items().size() != expectedPageSize) {
                throw new IllegalStateException("东方财富资金流向分页数量不完整，page="
                        + pageNumber + "，expected=" + expectedPageSize
                        + "，actual=" + page.items().size());
            }
            for (JSONObject item : page.items()) {
                String stockCode = item.getString("f12");
                if (result.put(stockCode, item) != null) {
                    throw new IllegalStateException("东方财富资金流向存在重复股票：" + stockCode);
                }
            }
            pageNumber++;
        }
        log.info("东方财富资金流向分页获取完成，requestCount={}，sourceCount={}",
                pageNumber - 1, result.size());
        return result;
    }

    private URI buildUri(int pageNumber) {
        return UriComponentsBuilder.fromHttpUrl(sourceUrl)
                .queryParam("pn", pageNumber)
                .queryParam("pz", PAGE_SIZE)
                .queryParam("po", 1)
                .queryParam("np", 1)
                .queryParam("fltt", 2)
                .queryParam("invt", 2)
                .queryParam("fid", "f62")
                .queryParam("fs", MARKET_FILTER)
                .queryParam("fields", FIELDS)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private String request(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.REFERER, "https://data.eastmoney.com/zjlx/detail.html");
        headers.set(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT);
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9");

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            requestRateLimiter.acquire();
            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );
                String body = response.getBody();
                if (body == null || body.isBlank()) {
                    throw new IllegalStateException("东方财富资金流向响应为空");
                }
                log.info("东方财富资金流向请求完成，attempt={}，elapsedMs={}",
                        attempt, System.currentTimeMillis() - startTime);
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("东方财富资金流向请求失败，attempt={}，reason={}",
                        attempt, exception.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                }
            }
        }
        throw new IllegalStateException("东方财富资金流向请求失败", lastException);
    }

    private FundFlowPage parseResponse(String responseBody, int pageNumber) {
        try {
            JSONObject root = JSON.parseObject(responseBody);
            if (root == null || !Integer.valueOf(0).equals(root.getInteger("rc"))) {
                throw new IllegalStateException("东方财富资金流向响应状态异常");
            }
            JSONObject data = root.getJSONObject("data");
            JSONArray items = data == null ? null : data.getJSONArray("diff");
            Integer total = data == null ? null : data.getInteger("total");
            if (items == null || total == null || total < 1) {
                throw new IllegalStateException("东方财富资金流向响应缺少数据，page=" + pageNumber);
            }

            List<JSONObject> result = new ArrayList<>(items.size());
            for (int index = 0; index < items.size(); index++) {
                JSONObject item = items.getJSONObject(index);
                String stockCode = item == null ? null : item.getString("f12");
                String stockName = item == null ? null : item.getString("f14");
                validateStock(stockCode, stockName);
                result.add(item);
            }
            return new FundFlowPage(total, result);
        } catch (JSONException exception) {
            throw new IllegalStateException("东方财富资金流向响应格式错误", exception);
        }
    }

    private StockFundFlow parseFundFlow(
            LocalDate tradeDate,
            JSONObject item,
            LocalDateTime collectedAt
    ) {
        String stockCode = item.getString("f12");
        String stockName = item.getString("f14").strip();
        BigDecimal latestPrice = decimal(item, "f2", stockCode);
        BigDecimal changePercent = decimal(item, "f3", stockCode);
        BigDecimal mainNetInflowYuan = decimal(item, "f62", stockCode);
        BigDecimal mainNetInflowRatio = decimal(item, "f184", stockCode);
        BigDecimal superLargeNetInflowYuan = decimal(item, "f66", stockCode);
        BigDecimal superLargeNetInflowRatio = decimal(item, "f69", stockCode);
        BigDecimal largeNetInflowYuan = decimal(item, "f72", stockCode);
        BigDecimal largeNetInflowRatio = decimal(item, "f75", stockCode);
        boolean complete = Stream.of(
                latestPrice,
                changePercent,
                mainNetInflowYuan,
                mainNetInflowRatio,
                superLargeNetInflowYuan,
                superLargeNetInflowRatio,
                largeNetInflowYuan,
                largeNetInflowRatio
        ).allMatch(value -> value != null);

        return StockFundFlow.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .tradeDate(tradeDate)
                .latestPrice(latestPrice)
                .changePercent(changePercent)
                .mainNetInflowYuan(mainNetInflowYuan)
                .mainNetInflowRatio(mainNetInflowRatio)
                .superLargeNetInflowYuan(superLargeNetInflowYuan)
                .superLargeNetInflowRatio(superLargeNetInflowRatio)
                .largeNetInflowYuan(largeNetInflowYuan)
                .largeNetInflowRatio(largeNetInflowRatio)
                .dataSource("EAST_MONEY")
                .dataStatus(complete ? "COMPLETE" : "PARTIAL")
                .collectedAt(collectedAt)
                .build();
    }

    private BigDecimal decimal(JSONObject item, String field, String stockCode) {
        Object value = item.get(field);
        if (value == null || "-".equals(value) || "--".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("东方财富资金流向数值格式错误，stockCode="
                    + stockCode + "，field=" + field, exception);
        }
    }

    private void validateStock(String stockCode, String stockName) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalStateException("东方财富返回了无效股票代码：" + stockCode);
        }
        if (stockName == null || stockName.isBlank()) {
            throw new IllegalStateException("东方财富返回了空股票名称，stockCode=" + stockCode);
        }
    }

    private void backoff(int attempt) {
        LockSupport.parkNanos(RETRY_BACKOFF.multipliedBy(attempt).toNanos());
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("东方财富资金流向请求被中断");
        }
    }

    private record FundFlowPage(int total, List<JSONObject> items) {
    }

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
            long waitNanos = nextRequestNanos - System.nanoTime();
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("东方财富资金流向请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
