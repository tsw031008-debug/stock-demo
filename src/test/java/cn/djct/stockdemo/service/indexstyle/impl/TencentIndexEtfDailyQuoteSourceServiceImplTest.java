package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentIndexEtfDailyQuoteSourceServiceImplTest {

    private static final String SOURCE_URL = "https://example.com/quotes?codes=";
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final MediaType TENCENT_MEDIA_TYPE = new MediaType("text", "plain", GB18030);

    @Test
    void shouldParseFourEtfsInFixedOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String expectedUrl = SOURCE_URL
                + "sh510050,sh510300,sz159949,sh512100";
        server.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(completeResponse().getBytes(GB18030), TENCENT_MEDIA_TYPE));
        TencentIndexEtfDailyQuoteSourceServiceImpl sourceService =
                new TencentIndexEtfDailyQuoteSourceServiceImpl(
                        restTemplate,
                        SOURCE_URL,
                        Duration.ZERO
                );

        List<IndexEtfDailyQuote> result = sourceService.fetch(LocalDate.of(2026, 9, 4));

        assertEquals(List.of("510050", "510300", "159949", "512100"),
                result.stream().map(IndexEtfDailyQuote::getEtfCode).toList());
        assertEquals(0, new BigDecimal("3.1200").compareTo(result.get(0).getClosePrice()));
        assertEquals("上证50", result.get(0).getIndexName());
        server.verify();
    }

    @Test
    void shouldRejectMissingEtf() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String expectedUrl = SOURCE_URL
                + "sh510050,sh510300,sz159949,sh512100";
        server.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(
                        quoteLine(IndexEtf.SSE_50, "3.1200")
                                .getBytes(GB18030),
                        TENCENT_MEDIA_TYPE
                ));
        TencentIndexEtfDailyQuoteSourceServiceImpl sourceService =
                new TencentIndexEtfDailyQuoteSourceServiceImpl(
                        restTemplate,
                        SOURCE_URL,
                        Duration.ZERO
                );

        assertThrows(
                IllegalStateException.class,
                () -> sourceService.fetch(LocalDate.of(2026, 9, 4))
        );
        server.verify();
    }

    private String completeResponse() {
        return quoteLine(IndexEtf.CSI_1000, "2.5800")
                + quoteLine(IndexEtf.SSE_50, "3.1200")
                + quoteLine(IndexEtf.CHINEXT_50, "1.2600")
                + quoteLine(IndexEtf.CSI_300, "4.1100");
    }

    private String quoteLine(IndexEtf etf, String closePrice) {
        String[] fields = new String[49];
        Arrays.fill(fields, "");
        fields[0] = "1";
        fields[1] = etf.getIndexName() + "ETF";
        fields[2] = etf.getEtfCode();
        fields[3] = closePrice;
        fields[30] = "20260904150000";
        return "v_" + etf.getTencentSymbol() + "=\"" + String.join("~", fields) + "\";";
    }
}
