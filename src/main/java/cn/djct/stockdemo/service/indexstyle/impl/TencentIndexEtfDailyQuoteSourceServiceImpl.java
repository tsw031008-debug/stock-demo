package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSourceService;
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
 * 腾讯四只固定指数ETF日行情数据源。
 */
@Slf4j
@Service
public class TencentIndexEtfDailyQuoteSourceServiceImpl
        implements IndexEtfDailyQuoteSourceService {

    private static final int MAX_ATTEMPTS = 2;
    private static final int CURRENT_PRICE_FIELD_INDEX = 3;
    private static final int QUOTE_TIME_FIELD_INDEX = 30;
    private static final Charset TENCENT_CHARSET = Charset.forName("GB18030");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter QUOTE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern QUOTE_PATTERN = Pattern.compile(
            "v_(sh510050|sh510300|sz159949|sh512100)=\"([^\"]*)\";"
    );

    private final RestTemplate restTemplate;
    private final String sourceUrl;
    private final RequestRateLimiter requestRateLimiter;

    /**
     * 创建腾讯指数ETF日行情数据源。
     */
    @Autowired
    public TencentIndexEtfDailyQuoteSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.index-style.etf-daily-quote.source.url}") String sourceUrl,
            @Value("${stock.index-style.etf-daily-quote.source.connect-timeout:5s}")
            Duration connectTimeout,
            @Value("${stock.index-style.etf-daily-quote.source.read-timeout:10s}")
            Duration readTimeout,
            @Value("${stock.index-style.etf-daily-quote.source.request-interval:1s}")
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

    TencentIndexEtfDailyQuoteSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    /**
     * 一次请求四只固定ETF，并按产品指定顺序返回。
     */
    @Override
    public List<IndexEtfDailyQuote> fetch(LocalDate tradeDate) {
        List<String> symbols = Arrays.stream(IndexEtf.values())
                .map(IndexEtf::getTencentSymbol)
                .toList();
        String responseText = new String(
                request(URI.create(sourceUrl + String.join(",", symbols))),
                TENCENT_CHARSET
        );
        Map<String, String[]> fieldsBySymbol = parseResponse(responseText);
        if (fieldsBySymbol.size() != IndexEtf.values().length) {
            throw new IllegalStateException("腾讯指数ETF行情数量不完整，expected=4，actual="
                    + fieldsBySymbol.size());
        }

        LocalDateTime collectedAt = LocalDateTime.now(SHANGHAI_ZONE);
        List<IndexEtfDailyQuote> result = new ArrayList<>(IndexEtf.values().length);
        for (IndexEtf etf : IndexEtf.values()) {
            String[] fields = fieldsBySymbol.get(etf.getTencentSymbol());
            if (fields == null) {
                throw new IllegalStateException("腾讯指数ETF行情缺失，etfCode="
                        + etf.getEtfCode());
            }
            result.add(parseQuote(etf, tradeDate, fields, collectedAt));
        }
        return result;
    }

    private Map<String, String[]> parseResponse(String responseText) {
        Matcher matcher = QUOTE_PATTERN.matcher(responseText);
        Map<String, String[]> result = new HashMap<>();
        while (matcher.find()) {
            String symbol = matcher.group(1);
            String[] fields = matcher.group(2).split("~", -1);
            if (fields.length <= QUOTE_TIME_FIELD_INDEX) {
                throw new IllegalStateException("腾讯指数ETF行情字段数量不足，symbol=" + symbol);
            }
            if (result.put(symbol, fields) != null) {
                throw new IllegalStateException("腾讯指数ETF行情重复，symbol=" + symbol);
            }
        }
        return result;
    }

    private IndexEtfDailyQuote parseQuote(
            IndexEtf etf,
            LocalDate expectedTradeDate,
            String[] fields,
            LocalDateTime collectedAt
    ) {
        String actualCode = requiredText(fields, 2, etf.getEtfCode(), "ETF代码");
        if (!etf.getEtfCode().equals(actualCode)) {
            throw new IllegalStateException("腾讯指数ETF代码不一致，expected="
                    + etf.getEtfCode() + "，actual=" + actualCode);
        }
        BigDecimal closePrice = positiveDecimal(
                fields,
                CURRENT_PRICE_FIELD_INDEX,
                etf.getEtfCode(),
                "收盘价"
        );
        LocalDateTime quoteTime = parseQuoteTime(fields[QUOTE_TIME_FIELD_INDEX], etf.getEtfCode());
        if (!expectedTradeDate.equals(quoteTime.toLocalDate())) {
            throw new IllegalStateException("腾讯指数ETF行情日期不一致，etfCode="
                    + etf.getEtfCode() + "，expected=" + expectedTradeDate
                    + "，actual=" + quoteTime.toLocalDate());
        }
        return IndexEtfDailyQuote.builder()
                .etfCode(etf.getEtfCode())
                .indexName(etf.getIndexName())
                .tradeDate(expectedTradeDate)
                .quoteTime(quoteTime)
                .closePrice(closePrice)
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
                    throw new IllegalStateException("腾讯指数ETF行情响应为空");
                }
                log.info("腾讯四只指数ETF行情请求完成，attempt={}，elapsedMs={}",
                        attempt, System.currentTimeMillis() - startTime);
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("腾讯指数ETF行情请求失败，attempt={}，reason={}",
                        attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("腾讯指数ETF行情请求失败", lastException);
    }

    private String requiredText(
            String[] fields,
            int fieldIndex,
            String etfCode,
            String fieldName
    ) {
        String value = fields[fieldIndex].strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("腾讯指数ETF" + fieldName + "为空，etfCode="
                    + etfCode);
        }
        return value;
    }

    private BigDecimal positiveDecimal(
            String[] fields,
            int fieldIndex,
            String etfCode,
            String fieldName
    ) {
        String value = requiredText(fields, fieldIndex, etfCode, fieldName);
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() <= 0) {
                throw new IllegalStateException("腾讯指数ETF" + fieldName
                        + "必须大于0，etfCode=" + etfCode);
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("腾讯指数ETF" + fieldName
                    + "格式错误，etfCode=" + etfCode, exception);
        }
    }

    private LocalDateTime parseQuoteTime(String value, String etfCode) {
        try {
            return LocalDateTime.parse(value.strip(), QUOTE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("腾讯指数ETF行情时间格式错误，etfCode="
                    + etfCode, exception);
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
                    throw new IllegalStateException("腾讯指数ETF行情请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
