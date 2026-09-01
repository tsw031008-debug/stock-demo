package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
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

class ThsStockPlateSourceServiceImplTest {

    private static final String SOURCE_URL = "http://example.test/plates";

    @Test
    void shouldParseAndDeduplicatePlateMembers() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo(SOURCE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":200,\"data\":{" +
                                "\"科技-概\":[\"000001\",\"000001\",\"600000\"]," +
                                "\"金融-概\":[\"000002\"]}}",
                        MediaType.APPLICATION_JSON
                ));
        ThsStockPlateSourceServiceImpl service =
                new ThsStockPlateSourceServiceImpl(restTemplate, SOURCE_URL);

        List<StockPlateSourceDto> result = service.fetchAll();

        assertEquals(List.of("科技-概", "金融-概"), result.stream()
                .map(StockPlateSourceDto::getPlateName)
                .toList());
        assertEquals(List.of("000001", "600000"), result.get(0).getStockCodes());
        server.verify();
    }

    @Test
    void shouldKeepFullWidthAndHalfWidthPlateNamesSeparate() {
        ThsStockPlateSourceServiceImpl service = createService(
                "{\"code\":200,\"data\":{" +
                        "\"东数西算(算力)-概\":[\"000001\"]," +
                        "\"东数西算（算力）-概\":[\"000002\"]}}"
        );

        List<StockPlateSourceDto> result = service.fetchAll();

        assertEquals(2, result.size());
        assertEquals(List.of("东数西算(算力)-概", "东数西算（算力）-概"), result.stream()
                .map(StockPlateSourceDto::getPlateName)
                .toList());
    }

    @Test
    void shouldRejectAbnormalBusinessStatus() {
        ThsStockPlateSourceServiceImpl service = createService("{\"code\":500,\"data\":{}}");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchAll
        );

        assertEquals("板块来源响应状态异常", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidStockCode() {
        ThsStockPlateSourceServiceImpl service = createService(
                "{\"code\":200,\"data\":{\"科技-概\":[\"ABC\"]}}"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::fetchAll
        );

        assertEquals("板块股票代码格式错误，plateName=科技-概", exception.getMessage());
    }

    /**
     * 使用固定响应创建板块数据源服务。
     */
    private ThsStockPlateSourceServiceImpl createService(String responseBody) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo(SOURCE_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return new ThsStockPlateSourceServiceImpl(restTemplate, SOURCE_URL);
    }
}
