package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentIndexMinuteQuoteSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/shanghai-composite";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final MediaType TENCENT_MEDIA_TYPE = new MediaType("text", "plain", GB18030);

    @Test
    void shouldParseShanghaiCompositeQuote() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(
                        quoteLine("000001", "上证指数", "3850.12", "3820.10", "20260827093145")
                                .getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        IndexMinuteQuote quote = sourceService.fetchShanghaiComposite();

        assertEquals("000001", quote.getIndexCode());
        assertEquals("上证指数", quote.getIndexName());
        assertEquals(LocalDateTime.of(2026, 8, 27, 9, 31, 45), quote.getQuoteTime());
        assertEquals(0, new BigDecimal("3850.12").compareTo(quote.getCurrentPrice()));
        assertEquals(0, new BigDecimal("3820.10").compareTo(quote.getPreviousClosePrice()));
        assertEquals("TENCENT", quote.getDataSource());
        server.verify();
    }

    @Test
    void shouldRejectEmptyResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(new byte[0], TENCENT_MEDIA_TYPE));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchShanghaiComposite);
        server.verify();
    }

    @Test
    void shouldRejectMissingFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(
                        "v_sh000001=\"1~上证指数~000001\";".getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchShanghaiComposite);
        server.verify();
    }

    @Test
    void shouldRejectNonPositivePrice() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(
                        quoteLine("000001", "上证指数", "0", "3820.10", "20260827093145")
                                .getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchShanghaiComposite);
        server.verify();
    }

    @Test
    void shouldRejectInvalidQuoteTime() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(
                        quoteLine("000001", "上证指数", "3850.12", "3820.10", "invalid")
                                .getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchShanghaiComposite);
        server.verify();
    }

    @Test
    void shouldRejectGarbledResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(SOURCE_URL))
                .andRespond(withSuccess(new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00},
                        TENCENT_MEDIA_TYPE));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class, sourceService::fetchShanghaiComposite);
        server.verify();
    }

    private TencentIndexMinuteQuoteSourceServiceImpl createSourceService(RestTemplate restTemplate) {
        return new TencentIndexMinuteQuoteSourceServiceImpl(restTemplate, SOURCE_URL, Duration.ZERO);
    }

    private String quoteLine(
            String code,
            String name,
            String currentPrice,
            String previousClosePrice,
            String quoteTime
    ) {
        String[] fields = new String[49];
        Arrays.fill(fields, "");
        fields[0] = "1";
        fields[1] = name;
        fields[2] = code;
        fields[3] = currentPrice;
        fields[4] = previousClosePrice;
        fields[30] = quoteTime;
        return "v_sh000001=\"" + String.join("~", fields) + "\";";
    }
}
