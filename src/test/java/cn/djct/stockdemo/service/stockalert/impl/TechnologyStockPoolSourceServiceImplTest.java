package cn.djct.stockdemo.service.stockalert.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TechnologyStockPoolSourceServiceImplTest {

    private static final String SOURCE_URL =
            "http://example.test/technologyStocks?plateId=676";

    @Test
    void shouldParseAndDeduplicateStockCodes() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo(SOURCE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":200,\"data\":[\"000001\",\"600000\",\"000001\"]}",
                        MediaType.APPLICATION_JSON
                ));
        TechnologyStockPoolSourceServiceImpl service =
                new TechnologyStockPoolSourceServiceImpl(restTemplate, SOURCE_URL);

        List<String> result = service.fetchStockCodes();

        assertEquals(List.of("000001", "600000"), result);
        server.verify();
    }

    @Test
    void shouldRejectAbnormalBusinessStatus() {
        TechnologyStockPoolSourceServiceImpl service = createService(
                "{\"code\":500,\"data\":[]}"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchStockCodes
        );

        assertEquals("科技股候选池响应状态异常", exception.getMessage());
    }

    @Test
    void shouldRejectEmptyStockCodes() {
        TechnologyStockPoolSourceServiceImpl service = createService(
                "{\"code\":200,\"data\":[]}"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchStockCodes
        );

        assertEquals("科技股候选池数据为空", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidStockCode() {
        TechnologyStockPoolSourceServiceImpl service = createService(
                "{\"code\":200,\"data\":[\"ABC\"]}"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchStockCodes
        );

        assertEquals("科技股候选池存在非法股票代码", exception.getMessage());
    }

    @Test
    void shouldRejectMalformedResponse() {
        TechnologyStockPoolSourceServiceImpl service = createService("not-json");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchStockCodes
        );

        assertEquals("科技股候选池响应格式错误", exception.getMessage());
    }

    private TechnologyStockPoolSourceServiceImpl createService(String responseBody) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo(SOURCE_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return new TechnologyStockPoolSourceServiceImpl(restTemplate, SOURCE_URL);
    }
}
