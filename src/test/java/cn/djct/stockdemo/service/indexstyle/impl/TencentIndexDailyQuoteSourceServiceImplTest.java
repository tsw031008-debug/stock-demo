package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentIndexDailyQuoteSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/?q=";
    private static final String EXPECTED_URL = SOURCE_URL
            + "sh000016,sh000001,sz399102,sh000852";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final MediaType TENCENT_MEDIA_TYPE = new MediaType("text", "plain", GB18030);

    @Test
    void shouldFetchFourIndicesInFixedOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(response(true).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentIndexDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        List<IndexDailyQuote> quotes = sourceService.fetch(LocalDate.of(2026, 9, 3));

        assertEquals(List.of("000016", "000001", "399102", "000852"),
                quotes.stream().map(IndexDailyQuote::getIndexCode).toList());
        assertEquals("上证50", quotes.get(0).getIndexName());
        assertEquals("TENCENT", quotes.get(0).getDataSource());
        server.verify();
    }

    @Test
    void shouldRejectMissingIndex() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(response(false).getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentIndexDailyQuoteSourceServiceImpl sourceService = createSourceService(restTemplate);

        assertThrows(IllegalStateException.class,
                () -> sourceService.fetch(LocalDate.of(2026, 9, 3)));
        server.verify();
    }

    private TencentIndexDailyQuoteSourceServiceImpl createSourceService(RestTemplate restTemplate) {
        return new TencentIndexDailyQuoteSourceServiceImpl(
                restTemplate,
                SOURCE_URL,
                Duration.ZERO
        );
    }

    private String response(boolean complete) {
        String response = quoteLine("sh000016", "000016", "上证50", "3000", "2990")
                + quoteLine("sh000001", "000001", "上证指数", "3800", "3790")
                + quoteLine("sz399102", "399102", "创业板综", "3500", "3510");
        if (complete) {
            response += quoteLine("sh000852", "000852", "中证1000", "7000", "6990");
        }
        return response;
    }

    private String quoteLine(
            String symbol,
            String code,
            String name,
            String closePrice,
            String previousClosePrice
    ) {
        String[] fields = new String[49];
        Arrays.fill(fields, "");
        fields[0] = "1";
        fields[1] = name;
        fields[2] = code;
        fields[3] = closePrice;
        fields[4] = previousClosePrice;
        fields[30] = "20260903150600";
        return "v_" + symbol + "=\"" + String.join("~", fields) + "\";";
    }
}
