package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.util.StockMarketCodeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentStockDailyQuoteSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/q=";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final MediaType TENCENT_MEDIA_TYPE = new MediaType("text", "plain", GB18030);
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void shouldParseTencentFieldsAndConvertUnits() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("q=sh600519")))
                .andRespond(withSuccess(
                        quoteLine("600519", "贵州茅台", null).getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentStockDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        StockDailyQuote quote = sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ).get(0);

        assertEquals("600519", quote.getStockCode());
        assertEquals("贵州茅台", quote.getStockName());
        assertEquals(LocalDateTime.of(2026, 8, 20, 15, 2, 1), quote.getQuoteTime());
        assertEquals(new BigDecimal("10.50"), quote.getClosePrice());
        assertEquals(12345L, quote.getVolumeHand());
        assertEquals(new BigDecimal("1234500.00"), quote.getTurnoverAmountYuan());
        assertEquals(new BigDecimal("10050000000.0"), quote.getCirculatingMarketCapYuan());
        assertEquals(new BigDecimal("12050000000.0"), quote.getTotalMarketCapYuan());
        assertEquals("COMPLETE", quote.getDataStatus());
        server.verify();
    }

    @Test
    void shouldSplitMoreThanTwoHundredStocksIntoMultipleRequests() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        List<StockBasic> stocks = new ArrayList<>();
        for (int index = 0; index < 201; index++) {
            stocks.add(createStock(String.format("600%03d", index), "股票" + index));
        }
        server.expect(requestTo(allOf(
                        containsString("q=sh600000"),
                        containsString("sh600199")
                )))
                .andRespond(withSuccess(responseFor(stocks.subList(0, 200)).getBytes(GB18030),
                        TENCENT_MEDIA_TYPE));
        server.expect(requestTo(containsString("q=sh600200")))
                .andRespond(withSuccess(responseFor(stocks.subList(200, 201)).getBytes(GB18030),
                        TENCENT_MEDIA_TYPE));
        TencentStockDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertEquals(201, sourceService.fetchAll(TRADE_DATE, stocks).size());
        server.verify();
    }

    @Test
    void shouldRejectChangedDuplicateHighField() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("q=sh600519")))
                .andRespond(withSuccess(
                        quoteLine("600519", "贵州茅台", fields -> fields[41] = "11.01").getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentStockDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, () -> sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ));
        server.verify();
    }

    @Test
    void shouldMarkQuotePartialWhenFieldIsMissing() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("q=sh600519")))
                .andRespond(withSuccess(
                        quoteLine("600519", "贵州茅台", fields -> fields[39] = "").getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentStockDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        StockDailyQuote quote = sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ).get(0);

        assertEquals("PARTIAL", quote.getDataStatus());
        server.verify();
    }

    private TencentStockDailyQuoteSourceServiceImpl createSourceService(RestTemplate restTemplate) {
        return new TencentStockDailyQuoteSourceServiceImpl(
                restTemplate,
                SOURCE_URL,
                Duration.ZERO
        );
    }

    private StockBasic createStock(String stockCode, String stockName) {
        return StockBasic.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .lastSeenTradeDate(TRADE_DATE)
                .build();
    }

    private String responseFor(List<StockBasic> stocks) {
        return stocks.stream()
                .map(stock -> quoteLine(stock.getStockCode(), stock.getStockName(), null))
                .reduce("", (left, right) -> left + right + "\n");
    }

    private String quoteLine(String stockCode, String stockName, FieldCustomizer customizer) {
        String[] fields = new String[49];
        Arrays.fill(fields, "");
        fields[0] = "1";
        fields[1] = stockName;
        fields[2] = stockCode;
        fields[3] = "10.50";
        fields[4] = "10.00";
        fields[5] = "10.10";
        fields[6] = "12345";
        fields[9] = "10.49";
        fields[10] = "100";
        fields[19] = "10.50";
        fields[20] = "120";
        fields[30] = "20260820150201";
        fields[32] = "5.00";
        fields[33] = "11.00";
        fields[34] = "9.80";
        fields[36] = "12345";
        fields[37] = "123.45";
        fields[38] = "1.20";
        fields[39] = "15.50";
        fields[41] = "11.00";
        fields[42] = "9.80";
        fields[43] = "12.00";
        fields[44] = "100.5";
        fields[45] = "120.5";
        fields[46] = "1.80";
        if (customizer != null) {
            customizer.customize(fields);
        }
        return "v_" + StockMarketCodeUtil.toTencentSymbol(stockCode)
                + "=\"" + String.join("~", fields) + "\";";
    }

    @FunctionalInterface
    private interface FieldCustomizer {
        void customize(String[] fields);
    }
}
