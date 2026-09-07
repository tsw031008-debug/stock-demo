package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentIndexMinuteQuoteSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/shanghai-composite";
    private static final String HISTORY_URL = "https://example.com/shanghai-composite/minutes";
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

    @Test
    void shouldParseShanghaiCompositeMinutes() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(HISTORY_URL))
                .andRespond(withSuccess(
                        minuteResponse(List.of(
                                "0930 3848.10 100 1000.00",
                                "0931 3850.12 200 2000.00",
                                "0932 3851.20 300 3000.00"
                        )).getBytes(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_JSON
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService =
                createSourceService(restTemplate, HISTORY_URL);

        List<IndexMinuteQuote> quotes = sourceService.fetchShanghaiCompositeMinutes();

        assertEquals(3, quotes.size());
        assertEquals(LocalDateTime.of(2026, 8, 27, 9, 31), quotes.get(1).getQuoteTime());
        assertEquals(0, new BigDecimal("3850.12").compareTo(quotes.get(1).getCurrentPrice()));
        assertEquals(0, new BigDecimal("3820.10")
                .compareTo(quotes.get(1).getPreviousClosePrice()));
        server.verify();
    }

    @Test
    void shouldRejectNonPositiveMinutePrice() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(HISTORY_URL))
                .andRespond(withSuccess(
                        minuteResponse(List.of("0931 0 200 2000.00"))
                                .getBytes(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_JSON
                ));
        TencentIndexMinuteQuoteSourceServiceImpl sourceService =
                createSourceService(restTemplate, HISTORY_URL);

        assertThrows(
                IllegalStateException.class,
                sourceService::fetchShanghaiCompositeMinutes
        );
        server.verify();
    }

    private TencentIndexMinuteQuoteSourceServiceImpl createSourceService(RestTemplate restTemplate) {
        return new TencentIndexMinuteQuoteSourceServiceImpl(restTemplate, SOURCE_URL, Duration.ZERO);
    }

    private TencentIndexMinuteQuoteSourceServiceImpl createSourceService(
            RestTemplate restTemplate,
            String historyUrl
    ) {
        return new TencentIndexMinuteQuoteSourceServiceImpl(
                restTemplate,
                SOURCE_URL,
                historyUrl,
                Duration.ZERO
        );
    }

    private String minuteResponse(List<String> rows) {
        return """
                {
                  "code": 0,
                  "data": {
                    "sh000001": {
                      "data": {
                        "data": %s,
                        "date": "20260827"
                      },
                      "qt": {
                        "sh000001": ["1", "上证指数", "000001", "3850.12", "3820.10"]
                      }
                    }
                  }
                }
                """.formatted(com.alibaba.fastjson2.JSON.toJSONString(rows));
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
