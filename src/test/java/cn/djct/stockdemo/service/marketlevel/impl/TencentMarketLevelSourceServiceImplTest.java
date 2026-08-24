package cn.djct.stockdemo.service.marketlevel.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentMarketLevelSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/market-level";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final MediaType TENCENT_MEDIA_TYPE = new MediaType("text", "plain", GB18030);

    @Test
    void shouldParseBothIndexesRegardlessOfResponseOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess((quoteLine("sz399106", "2445973")
                        + quoteLine("sh000001", "29234524")).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertEquals(new BigDecimal("316804970000"), sourceService.fetchCurrentTurnoverAmountYuan());
        server.verify();
    }

    @Test
    void shouldRejectResponseMissingOneIndex() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(quoteLine("sh000001", "29234524").getBytes(GB18030),
                        TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    @Test
    void shouldRejectNegativeTurnoverAmount() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess((quoteLine("sh000001", "-1")
                        + quoteLine("sz399106", "2445973")).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    @Test
    void shouldRejectEmptyResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(new byte[0], TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    @Test
    void shouldRejectMissingTurnoverField() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(("v_s_sh000001=\"1~上证指数~000001\";"
                        + quoteLine("sz399106", "2445973")).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    @Test
    void shouldRejectNonNumericTurnoverAmount() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess((quoteLine("sh000001", "changed")
                        + quoteLine("sz399106", "2445973")).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    @Test
    void shouldRejectGarbledResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00}, TENCENT_MEDIA_TYPE));
        TencentMarketLevelSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchCurrentTurnoverAmountYuan);
        server.verify();
    }

    private TencentMarketLevelSourceServiceImpl createSourceService(RestTemplate restTemplate) {
        return new TencentMarketLevelSourceServiceImpl(restTemplate, SOURCE_URL, Duration.ZERO);
    }

    private String quoteLine(String symbol, String turnoverAmountWan) {
        String[] fields = new String[10];
        Arrays.fill(fields, "");
        fields[0] = "1";
        fields[1] = symbol.startsWith("sh") ? "上证指数" : "深证综指";
        fields[2] = symbol.substring(2);
        fields[3] = "3000.00";
        fields[7] = turnoverAmountWan;
        return "v_s_" + symbol + "=\"" + String.join("~", fields) + "\";";
    }
}
