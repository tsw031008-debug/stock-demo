package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSourceService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯上证指数分钟行情数据源。
 */
@Slf4j
@Service
public class TencentIndexMinuteQuoteSourceServiceImpl implements IndexMinuteQuoteSourceService {

    private static final int MAX_ATTEMPTS = 2;
    private static final int CURRENT_PRICE_FIELD_INDEX = 3;
    private static final int PREVIOUS_CLOSE_FIELD_INDEX = 4;
    private static final int QUOTE_TIME_FIELD_INDEX = 30;
    private static final String SHANGHAI_COMPOSITE_CODE = "000001";
    private static final Charset TENCENT_CHARSET = Charset.forName("GB18030");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter QUOTE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern QUOTE_PATTERN = Pattern.compile(
            "v_sh000001=\"([^\"]*)\";"
    );

    private final RestTemplate restTemplate;
    private final URI sourceUri;
    private final RequestRateLimiter requestRateLimiter;

    /**
     * 创建腾讯指数分钟行情数据源。
     */
    @Autowired
    public TencentIndexMinuteQuoteSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.index-divergence.minute-quote.source.url}") String sourceUrl,
            @Value("${stock.index-divergence.minute-quote.source.connect-timeout:5s}")
            Duration connectTimeout,
            @Value("${stock.index-divergence.minute-quote.source.read-timeout:10s}")
            Duration readTimeout,
            @Value("${stock.index-divergence.minute-quote.source.request-interval:1s}")
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

    TencentIndexMinuteQuoteSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUri = URI.create(sourceUrl);
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    /**
     * 获取并解析上证指数当前行情。
     */
    @Override
    public IndexMinuteQuote fetchShanghaiComposite() {
        String responseText = new String(request(), TENCENT_CHARSET);
        Matcher matcher = QUOTE_PATTERN.matcher(responseText);
        if (!matcher.find()) {
            throw new IllegalStateException("腾讯上证指数行情格式错误");
        }
        String[] fields = matcher.group(1).split("~", -1);
        if (fields.length <= QUOTE_TIME_FIELD_INDEX) {
            throw new IllegalStateException("腾讯上证指数行情字段数量不足");
        }

        String indexName = requiredText(fields, 1, "指数名称");
        String indexCode = requiredText(fields, 2, "指数代码");
        if (!SHANGHAI_COMPOSITE_CODE.equals(indexCode)) {
            throw new IllegalStateException("腾讯上证指数代码错误，actual=" + indexCode);
        }
        // 当前价格
        BigDecimal currentPrice = positiveDecimal(
                fields,
                CURRENT_PRICE_FIELD_INDEX,
                "当前价格"
        );
        // 昨收价
        BigDecimal previousClosePrice = positiveDecimal(
                fields,
                PREVIOUS_CLOSE_FIELD_INDEX,
                "昨收价"
        );
        // 行情时间
        LocalDateTime quoteTime = parseQuoteTime(fields[QUOTE_TIME_FIELD_INDEX]);
        // 收集时间
        LocalDateTime collectedAt = LocalDateTime.now(SHANGHAI_ZONE);

        return IndexMinuteQuote.builder()
                .indexCode(indexCode)
                .indexName(indexName)
                .tradeDate(quoteTime.toLocalDate())
                .quoteTime(quoteTime)
                .currentPrice(currentPrice)
                .previousClosePrice(previousClosePrice)
                .dataSource("TENCENT")
                .collectedAt(collectedAt)
                .build();
    }

    /**
     * 请求腾讯上证指数行情，失败时最多重试一次。
     */
    private byte[] request() {
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
                        sourceUri,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                byte[] body = response.getBody();
                if (body == null || body.length == 0) {
                    throw new IllegalStateException("腾讯上证指数行情响应为空");
                }
                log.info("腾讯上证指数行情请求完成，attempt={}，elapsedMs={}",
                        attempt, System.currentTimeMillis() - startTime);
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("腾讯上证指数行情请求失败，attempt={}，reason={}",
                        attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("腾讯上证指数行情请求失败", lastException);
    }

    private String requiredText(String[] fields, int index, String fieldName) {
        String value = fields[index].strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("腾讯上证指数" + fieldName + "为空");
        }
        return value;
    }

    private BigDecimal positiveDecimal(String[] fields, int index, String fieldName) {
        String value = requiredText(fields, index, fieldName);
        try {
            BigDecimal number = new BigDecimal(value);
            // 大于0
            if (number.signum() <= 0) {
                throw new IllegalStateException("腾讯上证指数" + fieldName + "必须大于0");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("腾讯上证指数" + fieldName + "格式错误", exception);
        }
    }

    private LocalDateTime parseQuoteTime(String value) {
        try {
            return LocalDateTime.parse(value.strip(), QUOTE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("腾讯上证指数行情时间格式错误", exception);
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
                    throw new IllegalStateException("腾讯上证指数行情请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
