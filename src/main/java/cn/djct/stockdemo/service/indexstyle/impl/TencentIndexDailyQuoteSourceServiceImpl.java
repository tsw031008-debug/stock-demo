package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSourceService;
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

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯四指数日行情数据源。
 */
@Slf4j
@Service
public class TencentIndexDailyQuoteSourceServiceImpl implements IndexDailyQuoteSourceService {

    private static final int MAX_ATTEMPTS = 2;
    private static final int QUOTE_TIME_FIELD_INDEX = 30;
    private static final Charset TENCENT_CHARSET = Charset.forName("GB18030");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter QUOTE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern QUOTE_PATTERN = Pattern.compile(
            "v_(sh000016|sh000001|sz399102|sh000852)=\"([^\"]*)\";"
    );

    private final RestTemplate restTemplate;
    private final String sourceUrl;
    private final RequestRateLimiter requestRateLimiter;

    /**
     * 创建腾讯四指数日行情数据源。
     */
    @Autowired
    public TencentIndexDailyQuoteSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.index-style.daily-quote.source.url}") String sourceUrl,
            @Value("${stock.index-style.daily-quote.source.connect-timeout:5s}")
            Duration connectTimeout,
            @Value("${stock.index-style.daily-quote.source.read-timeout:10s}")
            Duration readTimeout,
            @Value("${stock.index-style.daily-quote.source.request-interval:1s}")
            Duration requestInterval
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

    TencentIndexDailyQuoteSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    /**
     * 一次请求四个固定指数，并按产品固定展示顺序返回。
     */
    @Override
    public List<IndexDailyQuote> fetch(LocalDate tradeDate) {
        List<String> symbols = Arrays.stream(IndexStyleIndex.values())
                .map(IndexStyleIndex::getTencentSymbol)
                .toList();
        URI uri = URI.create(sourceUrl + String.join(",", symbols));
        String responseText = new String(request(uri), TENCENT_CHARSET);
        Map<String, String[]> fieldsBySymbol = parseResponse(responseText);
        if (fieldsBySymbol.size() != IndexStyleIndex.values().length) {
            throw new IllegalStateException("腾讯指数行情数量不完整，expected=4，actual="
                    + fieldsBySymbol.size());
        }

        LocalDateTime collectedAt = LocalDateTime.now(SHANGHAI_ZONE);
        List<IndexDailyQuote> quotes = new ArrayList<>(IndexStyleIndex.values().length);
        for (IndexStyleIndex index : IndexStyleIndex.values()) {
            String[] fields = fieldsBySymbol.get(index.getTencentSymbol());
            if (fields == null) {
                throw new IllegalStateException("腾讯指数行情缺失，indexCode=" + index.getIndexCode());
            }
            quotes.add(parseQuote(index, tradeDate, fields, collectedAt));
        }
        return quotes;
    }

    private Map<String, String[]> parseResponse(String responseText) {
        Matcher matcher = QUOTE_PATTERN.matcher(responseText);
        Map<String, String[]> result = new HashMap<>();
        while (matcher.find()) {
            String symbol = matcher.group(1);
            String[] fields = matcher.group(2).split("~", -1);
            if (fields.length <= QUOTE_TIME_FIELD_INDEX) {
                throw new IllegalStateException("腾讯指数行情字段数量不足，symbol=" + symbol);
            }
            if (result.put(symbol, fields) != null) {
                throw new IllegalStateException("腾讯指数行情重复，symbol=" + symbol);
            }
        }
        return result;
    }

    private IndexDailyQuote parseQuote(
            IndexStyleIndex index,
            LocalDate expectedTradeDate,
            String[] fields,
            LocalDateTime collectedAt
    ) {
        String actualCode = requiredText(fields, 2, index.getIndexCode(), "指数代码");
        if (!index.getIndexCode().equals(actualCode)) {
            throw new IllegalStateException("腾讯指数代码不一致，expected=" + index.getIndexCode()
                    + "，actual=" + actualCode);
        }
        BigDecimal closePrice = positiveDecimal(fields, 3, index.getIndexCode(), "收盘价");
        BigDecimal previousClosePrice = positiveDecimal(fields, 4, index.getIndexCode(), "昨收价");
        LocalDateTime quoteTime = parseQuoteTime(fields[QUOTE_TIME_FIELD_INDEX], index.getIndexCode());
        if (!expectedTradeDate.equals(quoteTime.toLocalDate())) {
            throw new IllegalStateException("腾讯指数行情日期不一致，indexCode=" + index.getIndexCode()
                    + "，expected=" + expectedTradeDate + "，actual=" + quoteTime.toLocalDate());
        }
        return IndexDailyQuote.builder()
                .indexCode(index.getIndexCode())
                .indexName(index.getIndexName())
                .tradeDate(expectedTradeDate)
                .quoteTime(quoteTime)
                .closePrice(closePrice)
                .previousClosePrice(previousClosePrice)
                .dataSource("TENCENT")
                .collectedAt(collectedAt)
                .build();
    }

    /**
     * 请求腾讯行情，失败时最多重试一次。
     */
    private byte[] request(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
        headers.set(HttpHeaders.REFERER, "https://gu.qq.com/");
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            requestRateLimiter.acquire();
            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                byte[] body = response.getBody();
                if (body == null || body.length == 0) {
                    throw new IllegalStateException("腾讯指数行情响应为空");
                }
                log.info("腾讯四指数行情请求完成，attempt={}，elapsedMs={}",
                        attempt, System.currentTimeMillis() - startTime);
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("腾讯四指数行情请求失败，attempt={}，reason={}",
                        attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("腾讯四指数行情请求失败", lastException);
    }

    private String requiredText(
            String[] fields,
            int fieldIndex,
            String indexCode,
            String fieldName
    ) {
        String value = fields[fieldIndex].strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("腾讯指数" + fieldName + "为空，indexCode=" + indexCode);
        }
        return value;
    }

    private BigDecimal positiveDecimal(
            String[] fields,
            int fieldIndex,
            String indexCode,
            String fieldName
    ) {
        String value = requiredText(fields, fieldIndex, indexCode, fieldName);
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() <= 0) {
                throw new IllegalStateException("腾讯指数" + fieldName + "必须大于0，indexCode="
                        + indexCode);
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("腾讯指数" + fieldName + "格式错误，indexCode="
                    + indexCode, exception);
        }
    }

    private LocalDateTime parseQuoteTime(String value, String indexCode) {
        try {
            return LocalDateTime.parse(value.strip(), QUOTE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("腾讯指数行情时间格式错误，indexCode=" + indexCode, exception);
        }
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
                    throw new IllegalStateException("腾讯指数行情请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
