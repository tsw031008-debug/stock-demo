package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.service.marketlevel.MarketLevelSourceService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯市场水位实时数据源。
 */
@Slf4j
@Service
public class TencentMarketLevelSourceServiceImpl implements MarketLevelSourceService {

    private static final int MAX_ATTEMPTS = 2;
    private static final int TURNOVER_FIELD_INDEX = 7;
    private static final Charset TENCENT_CHARSET = Charset.forName("GB18030");
    private static final Pattern QUOTE_PATTERN = Pattern.compile(
            "v_s_(sh000001|sz399106)=\"([^\"]*)\";"
    );
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    private final RestTemplate restTemplate;
    private final URI sourceUri;
    private final RequestRateLimiter requestRateLimiter;

    /**
     * 创建腾讯市场水位数据源。
     *
     * @param restTemplateBuilder RestTemplate构建器
     * @param sourceUrl           腾讯市场水位地址
     * @param connectTimeout      连接超时
     * @param readTimeout         读取超时
     * @param requestInterval     请求间隔
     */
    @Autowired
    public TencentMarketLevelSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.market-level.source.url}") String sourceUrl,
            @Value("${stock.market-level.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.market-level.source.read-timeout:10s}") Duration readTimeout,
            @Value("${stock.market-level.source.request-interval:1s}") Duration requestInterval
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

    /**
     * 创建使用指定RestTemplate的腾讯市场水位数据源，供固定样本测试使用。
     *
     * @param restTemplate   HTTP客户端
     * @param sourceUrl      腾讯市场水位地址
     * @param requestInterval 请求间隔
     */
    TencentMarketLevelSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUri = URI.create(sourceUrl);
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    /**
     * 获取当前沪深两市成交额。
     *
     * @return 当前沪深两市成交额，单位：元
     */
    @Override
    public BigDecimal fetchCurrentTurnoverAmountYuan() {
        // 腾讯响应使用GB18030编码
        String responseText = new String(request(), TENCENT_CHARSET);
        // 分别解析上证指数和深证综指成交额
        Map<String, BigDecimal> turnoverAmountsWan = parseResponse(responseText);
        if (turnoverAmountsWan.size() != 2) {
            throw new IllegalStateException("腾讯市场水位响应指数数量不完整，actual="
                    + turnoverAmountsWan.size());
        }
        // 两个指数成交额相加，并从万元转换为元
        return turnoverAmountsWan.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(TEN_THOUSAND);
    }

    /**
     * 请求腾讯市场水位数据。
     *
     * @return 原始响应字节
     */
    private byte[] request() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
        headers.set(HttpHeaders.REFERER, "https://gu.qq.com/");
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 限制请求频率，避免连续请求外部行情源
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
                    throw new IllegalStateException("腾讯市场水位响应为空");
                }
                log.info("腾讯市场水位请求完成，indexCount=2，attempt={}，elapsedMs={}",
                        attempt, System.currentTimeMillis() - startTime);
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("腾讯市场水位请求失败，indexCount=2，attempt={}，reason={}",
                        attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("腾讯市场水位请求失败", lastException);
    }

    /**
     * 解析腾讯上证指数和深证综指成交额。
     *
     * @param responseText 腾讯响应文本
     * @return 指数代码与成交额映射，成交额单位：万元
     */
    private Map<String, BigDecimal> parseResponse(String responseText) {
        Matcher matcher = QUOTE_PATTERN.matcher(responseText);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String symbol = matcher.group(1);
            String[] fields = matcher.group(2).split("~", -1);
            if (fields.length <= TURNOVER_FIELD_INDEX) {
                throw new IllegalStateException("腾讯市场水位字段数量不足，symbol=" + symbol);
            }
            BigDecimal turnoverAmountWan = parseTurnoverAmount(fields[TURNOVER_FIELD_INDEX], symbol);
            if (result.put(symbol, turnoverAmountWan) != null) {
                throw new IllegalStateException("腾讯市场水位存在重复指数，symbol=" + symbol);
            }
        }
        return result;
    }

    /**
     * 解析并校验单个指数成交额。
     *
     * @param value  腾讯成交额字段
     * @param symbol 腾讯指数代码
     * @return 成交额，单位：万元
     */
    private BigDecimal parseTurnoverAmount(String value, String symbol) {
        String normalizedValue = value.strip();
        if (normalizedValue.isEmpty() || "--".equals(normalizedValue)) {
            throw new IllegalStateException("腾讯市场水位成交额为空，symbol=" + symbol);
        }
        try {
            BigDecimal turnoverAmount = new BigDecimal(normalizedValue);
            if (turnoverAmount.signum() < 0) {
                throw new IllegalStateException("腾讯市场水位成交额不能小于0，symbol=" + symbol);
            }
            return turnoverAmount;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("腾讯市场水位成交额格式错误，symbol=" + symbol, exception);
        }
    }

    private static final class RequestRateLimiter {

        private final long intervalNanos;
        private long nextRequestNanos;

        /**
         * 创建请求限频器。
         *
         * @param interval 最小请求间隔
         */
        private RequestRateLimiter(Duration interval) {
            if (interval.isNegative()) {
                throw new IllegalArgumentException("请求间隔不能小于0");
            }
            this.intervalNanos = interval.toNanos();
        }

        /**
         * 等待下一个可请求时间点。
         */
        private synchronized void acquire() {
            long waitNanos = nextRequestNanos - System.nanoTime();
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("腾讯市场水位请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
