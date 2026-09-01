package cn.djct.stockdemo.service.stockfundflow.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EastMoneyStockFundFlowSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/fund-flow";
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 21);

    @Test
    void shouldParseFieldsWithoutChangingYuanUnits() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("pz=100")))
                .andExpect(header(HttpHeaders.USER_AGENT, containsString("Chrome/140.0.0.0")))
                .andExpect(header(HttpHeaders.REFERER,
                        "https://data.eastmoney.com/zjlx/detail.html"))
                .andRespond(withSuccess(response(item("600519", "贵州茅台", "2873727488")),
                        MediaType.APPLICATION_JSON));
        EastMoneyStockFundFlowSourceServiceImpl sourceService =
                new EastMoneyStockFundFlowSourceServiceImpl(restTemplate, SOURCE_URL);

        StockFundFlow fundFlow = sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ).get(0);

        assertEquals("600519", fundFlow.getStockCode());
        assertEquals("贵州茅台", fundFlow.getStockName());
        assertEquals(new BigDecimal("933.31"), fundFlow.getLatestPrice());
        assertEquals(new BigDecimal("3.22"), fundFlow.getChangePercent());
        assertEquals(new BigDecimal("2873727488"), fundFlow.getMainNetInflowYuan());
        assertEquals(new BigDecimal("13.20"), fundFlow.getMainNetInflowRatio());
        assertEquals(new BigDecimal("2480364032"), fundFlow.getSuperLargeNetInflowYuan());
        assertEquals(new BigDecimal("11.39"), fundFlow.getSuperLargeNetInflowRatio());
        assertEquals(new BigDecimal("393363456"), fundFlow.getLargeNetInflowYuan());
        assertEquals(new BigDecimal("1.81"), fundFlow.getLargeNetInflowRatio());
        assertEquals("EAST_MONEY", fundFlow.getDataSource());
        assertEquals("COMPLETE", fundFlow.getDataStatus());
        server.verify();
    }

    @Test
    void shouldKeepMissingSourceValueNullAndMarkPartial() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("pz=100")))
                .andRespond(withSuccess(response(item("600519", "贵州茅台", "-")),
                        MediaType.APPLICATION_JSON));
        EastMoneyStockFundFlowSourceServiceImpl sourceService =
                new EastMoneyStockFundFlowSourceServiceImpl(restTemplate, SOURCE_URL);

        StockFundFlow fundFlow = sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ).get(0);

        assertNull(fundFlow.getMainNetInflowYuan());
        assertEquals("PARTIAL", fundFlow.getDataStatus());
        server.verify();
    }

    @Test
    void shouldRejectTruncatedResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("pz=100")))
                .andRespond(withSuccess(
                        "{\"rc\":0,\"data\":{\"total\":2,\"diff\":["
                                + item("600519", "贵州茅台", "1") + "]}}",
                        MediaType.APPLICATION_JSON
                ));
        EastMoneyStockFundFlowSourceServiceImpl sourceService =
                new EastMoneyStockFundFlowSourceServiceImpl(restTemplate, SOURCE_URL);

        assertThrows(IllegalStateException.class, () -> sourceService.fetchAll(
                TRADE_DATE,
                List.of(createStock("600519", "贵州茅台"))
        ));
        server.verify();
    }

    @Test
    void shouldFetchAllPagesInOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        List<StockBasic> stocks = new ArrayList<>();
        List<String> items = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            String stockCode = String.format("%06d", index);
            String stockName = "股票" + index;
            stocks.add(createStock(stockCode, stockName));
            items.add(item(stockCode, stockName, "1"));
        }
        server.expect(requestTo(containsString("pn=1")))
                .andRespond(withSuccess(
                        response(101, String.join(",", items.subList(0, 100))),
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(containsString("pn=2")))
                .andRespond(withSuccess(response(101, items.get(100)), MediaType.APPLICATION_JSON));
        EastMoneyStockFundFlowSourceServiceImpl sourceService =
                new EastMoneyStockFundFlowSourceServiceImpl(
                        restTemplate,
                        SOURCE_URL,
                        Duration.ZERO
                );

        List<StockFundFlow> fundFlows = sourceService.fetchAll(TRADE_DATE, stocks);

        assertEquals(101, fundFlows.size());
        assertEquals("000100", fundFlows.get(100).getStockCode());
        server.verify();
    }

    private String response(String item) {
        return response(1, item);
    }

    private String response(int total, String items) {
        return "{\"rc\":0,\"data\":{\"total\":" + total + ",\"diff\":[" + items + "]}}";
    }

    private String item(String stockCode, String stockName, String mainNetInflow) {
        String mainValue = "-".equals(mainNetInflow) ? "\"-\"" : mainNetInflow;
        return "{\"f12\":\"" + stockCode + "\",\"f14\":\"" + stockName
                + "\",\"f2\":933.31,\"f3\":3.22,\"f62\":" + mainValue
                + ",\"f184\":13.20,\"f66\":2480364032,\"f69\":11.39"
                + ",\"f72\":393363456,\"f75\":1.81}";
    }

    private StockBasic createStock(String stockCode, String stockName) {
        return StockBasic.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .lastSeenTradeDate(TRADE_DATE)
                .build();
    }
}
