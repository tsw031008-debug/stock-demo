package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.util.StockMarketCodeUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 腾讯股票日行情数据源。
 */
@Slf4j
@Service
public class TencentStockDailyQuoteSourceServiceImpl implements StockDailyQuoteSourceService {

    private static final int MAX_SYMBOLS_PER_REQUEST = 200;
    private static final int MAX_ATTEMPTS = 2;
    private static final Charset TENCENT_CHARSET = Charset.forName("GB18030");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter QUOTE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("v_([a-z]{2}\\d{6})=\"([^\"]*)\";");
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");

    private final RestTemplate restTemplate;
    private final String sourceUrl;
    private final RequestRateLimiter requestRateLimiter;

    @Autowired
    public TencentStockDailyQuoteSourceServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${stock.daily-quote.source.url}") String sourceUrl,
            @Value("${stock.daily-quote.source.connect-timeout:5s}") Duration connectTimeout,
            @Value("${stock.daily-quote.source.read-timeout:10s}") Duration readTimeout,
            @Value("${stock.daily-quote.source.request-interval:1s}") Duration requestInterval
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

    TencentStockDailyQuoteSourceServiceImpl(
            RestTemplate restTemplate,
            String sourceUrl,
            Duration requestInterval
    ) {
        this.restTemplate = restTemplate;
        this.sourceUrl = sourceUrl;
        this.requestRateLimiter = new RequestRateLimiter(requestInterval);
    }

    // 批量获取股票行情
    @Override
    public List<StockDailyQuote> fetchAll(LocalDate tradeDate, List<StockBasic> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            throw new IllegalArgumentException("待查询股票不能为空");
        }

        return fetchByCodes(
                tradeDate,
                stocks.stream().map(StockBasic::getStockCode).toList()
        );
    }

    /**
     * 只查询调用方指定的股票代码，并按腾讯单次请求上限自动分批。
     */
    @Override
    public List<StockDailyQuote> fetchByCodes(LocalDate tradeDate, List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            throw new IllegalArgumentException("待查询股票代码不能为空");
        }

        List<StockDailyQuote> quotes = new ArrayList<>(stockCodes.size());
        for (int startIndex = 0; startIndex < stockCodes.size(); startIndex += MAX_SYMBOLS_PER_REQUEST) {
            int endIndex = Math.min(startIndex + MAX_SYMBOLS_PER_REQUEST, stockCodes.size());
            quotes.addAll(fetchBatch(tradeDate, stockCodes.subList(startIndex, endIndex)));
        }
        return quotes;
    }

    // 获取股票行情
    private List<StockDailyQuote> fetchBatch(LocalDate tradeDate, List<String> stockCodes) {
        List<String> symbols = stockCodes.stream()
                .map(StockMarketCodeUtil::toTencentSymbol)
                .toList();
        URI uri = URI.create(sourceUrl + String.join(",", symbols));
        // 发送HTTP请求获取响应
        byte[] responseBody = request(uri, symbols.size());
        // 解析HTTP响应
        String responseText = new String(responseBody, TENCENT_CHARSET);
        // 解析股票行情字段，确保字段顺序与腾讯行情响应字段顺序一致
        Map<String, String[]> quoteFields = parseResponse(responseText);

        if (quoteFields.size() != stockCodes.size()) {
            throw new IllegalStateException(
                    "腾讯行情响应数量不一致，expected=" + stockCodes.size()
                            + "，actual=" + quoteFields.size()
            );
        }

        // 获取行情数据，确保顺序与输入股票列表一致
        LocalDateTime collectedAt = LocalDateTime.now(SHANGHAI_ZONE);
        List<StockDailyQuote> quotes = new ArrayList<>(stockCodes.size());
        for (int index = 0; index < stockCodes.size(); index++) {
            String stockCode = stockCodes.get(index);
            String symbol = symbols.get(index);
            String[] fields = quoteFields.get(symbol);
            if (fields == null) {
                throw new IllegalStateException("腾讯行情缺少股票：" + stockCode);
            }
            quotes.add(parseQuote(tradeDate, stockCode, fields, collectedAt));
        }
        return quotes;
    }

    // 发送HTTP请求获取响应
    private byte[] request(URI uri, int stockCount) {
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
                    throw new IllegalStateException("腾讯行情响应为空");
                }
                log.info(
                        "腾讯行情批次请求完成，stockCount={}，attempt={}，elapsedMs={}",
                        stockCount,
                        attempt,
                        System.currentTimeMillis() - startTime
                );
                return body;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("腾讯行情批次请求失败，stockCount={}，attempt={}，reason={}",
                        stockCount, attempt, exception.getMessage());
            }
        }
        throw new IllegalStateException("腾讯行情批次请求失败，stockCount=" + stockCount, lastException);
    }

    // 解析HTTP响应
    private Map<String, String[]> parseResponse(String responseText) {
        Matcher matcher = QUOTE_PATTERN.matcher(responseText);
        Map<String, String[]> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String symbol = matcher.group(1);
            String[] fields = matcher.group(2).split("~", -1);
            if (fields.length <= 46) {
                throw new IllegalStateException("腾讯行情字段数量不足，symbol=" + symbol);
            }
            if (result.put(symbol, fields) != null) {
                throw new IllegalStateException("腾讯行情存在重复股票，symbol=" + symbol);
            }
        }
        return result;
    }

    // 解析股票行情字段，确保字段顺序与腾讯行情响应字段顺序一致
    private StockDailyQuote parseQuote(
            LocalDate tradeDate,
            String expectedStockCode,
            String[] fields,
            LocalDateTime collectedAt
    ) {
        String stockCode = text(fields, 2);
        String stockName = text(fields, 1);
        if (!expectedStockCode.equals(stockCode)) {
            throw new IllegalStateException("腾讯行情股票代码不一致，expected="
                    + expectedStockCode + "，actual=" + stockCode);
        }
        if (stockName == null) {
            throw new IllegalStateException("腾讯行情股票名称为空，stockCode=" + stockCode);
        }

        BigDecimal volume6 = decimal(fields, 6, stockCode);
        BigDecimal volume36 = decimal(fields, 36, stockCode);
        BigDecimal high33 = decimal(fields, 33, stockCode);
        BigDecimal high41 = decimal(fields, 41, stockCode);
        BigDecimal low34 = decimal(fields, 34, stockCode);
        BigDecimal low42 = decimal(fields, 42, stockCode);
        validateDuplicateField("成交量", volume6, volume36, stockCode);
        validateDuplicateField("最高价", high33, high41, stockCode);
        validateDuplicateField("最低价", low34, low42, stockCode);

        LocalDateTime quoteTime = quoteTime(fields, stockCode);
        BigDecimal closePrice = decimal(fields, 3, stockCode);
        BigDecimal previousClosePrice = decimal(fields, 4, stockCode);
        BigDecimal openPrice = decimal(fields, 5, stockCode);
        BigDecimal highPrice = firstNonNull(high33, high41);
        BigDecimal lowPrice = firstNonNull(low34, low42);
        Long volumeHand = longValue(firstNonNull(volume36, volume6), "成交量", stockCode);
        BigDecimal turnoverAmountYuan = multiply(decimal(fields, 37, stockCode), TEN_THOUSAND);
        BigDecimal bid1Price = decimal(fields, 9, stockCode);
        Long bid1VolumeHand = longValue(decimal(fields, 10, stockCode), "买一量", stockCode);
        BigDecimal ask1Price = decimal(fields, 19, stockCode);
        Long ask1VolumeHand = longValue(decimal(fields, 20, stockCode), "卖一量", stockCode);
        BigDecimal changePercent = decimal(fields, 32, stockCode);
        BigDecimal amplitudePercent = decimal(fields, 43, stockCode);
        BigDecimal turnoverRate = decimal(fields, 38, stockCode);
        BigDecimal peRatio = decimal(fields, 39, stockCode);
        BigDecimal pbRatio = decimal(fields, 46, stockCode);
        BigDecimal circulatingMarketCapYuan = multiply(decimal(fields, 44, stockCode), ONE_HUNDRED_MILLION);
        BigDecimal totalMarketCapYuan = multiply(decimal(fields, 45, stockCode), ONE_HUNDRED_MILLION);

        boolean complete = quoteTime != null
                && tradeDate.equals(quoteTime.toLocalDate())
                && Stream.of(
                closePrice, previousClosePrice, openPrice, highPrice, lowPrice,
                volumeHand, turnoverAmountYuan, bid1Price, bid1VolumeHand,
                ask1Price, ask1VolumeHand, changePercent, amplitudePercent,
                turnoverRate, peRatio, pbRatio, circulatingMarketCapYuan,
                totalMarketCapYuan
        ).allMatch(value -> value != null);

        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .tradeDate(tradeDate)
                .quoteTime(quoteTime)
                .closePrice(closePrice)
                .previousClosePrice(previousClosePrice)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .volumeHand(volumeHand)
                .turnoverAmountYuan(turnoverAmountYuan)
                .bid1Price(bid1Price)
                .bid1VolumeHand(bid1VolumeHand)
                .ask1Price(ask1Price)
                .ask1VolumeHand(ask1VolumeHand)
                .changePercent(changePercent)
                .amplitudePercent(amplitudePercent)
                .turnoverRate(turnoverRate)
                .peRatio(peRatio)
                .pbRatio(pbRatio)
                .circulatingMarketCapYuan(circulatingMarketCapYuan)
                .totalMarketCapYuan(totalMarketCapYuan)
                .dataSource("TENCENT")
                .dataStatus(complete ? "COMPLETE" : "PARTIAL")
                .collectedAt(collectedAt)
                .build();
    }

    private String text(String[] fields, int index) {
        String value = fields[index].strip();
        return value.isEmpty() ? null : value;
    }

    private BigDecimal decimal(String[] fields, int index, String stockCode) {
        String value = text(fields, index);
        if (value == null || "--".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("腾讯行情数值格式错误，stockCode="
                    + stockCode + "，fieldIndex=" + index, exception);
        }
    }

    private LocalDateTime quoteTime(String[] fields, String stockCode) {
        String value = text(fields, 30);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, QUOTE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("腾讯行情时间格式错误，stockCode=" + stockCode, exception);
        }
    }

    private void validateDuplicateField(
            String fieldName,
            BigDecimal first,
            BigDecimal second,
            String stockCode
    ) {
        if (first != null && second != null && first.compareTo(second) != 0) {
            throw new IllegalStateException("腾讯行情重复字段不一致，stockCode="
                    + stockCode + "，field=" + fieldName);
        }
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private Long longValue(BigDecimal value, String fieldName, String stockCode) {
        if (value == null) {
            return null;
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("腾讯行情整数格式错误，stockCode="
                    + stockCode + "，field=" + fieldName, exception);
        }
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal multiplier) {
        return value == null ? null : value.multiply(multiplier);
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

        // 获取请求令牌，确保请求间隔
        private synchronized void acquire() {
            long now = System.nanoTime();
            long waitNanos = nextRequestNanos - now;
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("腾讯行情请求被中断");
                }
            }
            nextRequestNanos = System.nanoTime() + intervalNanos;
        }
    }
}
