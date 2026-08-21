package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SinaStockBasicSourceServiceImplTest {

    private static final String COUNT_URL = "https://example.com/count";

    private static final String LIST_URL = "https://example.com/list";

    @Test
    void shouldFetchEveryStockReturnedByTheMarketEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("count?node=hs_a")))
                .andRespond(withSuccess("3", MediaType.TEXT_PLAIN));
        server.expect(requestTo(allOf(
                        containsString("list?page=1"),
                        containsString("num=100"),
                        containsString("node=hs_a")
                )))
                .andRespond(withSuccess(
                        """
                                [
                                  {symbol:"sh600000",code:"600000",name:"浦发银行"},
                                  {symbol:"sz000001",code:"000001",name:"平安银行"},
                                  {symbol:"bj920001",code:"920001",name:"北交样本"}
                                ]
                                """,
                        MediaType.APPLICATION_JSON
                ));
        SinaStockBasicSourceServiceImpl sourceService = new SinaStockBasicSourceServiceImpl(
                restTemplate,
                COUNT_URL,
                LIST_URL,
                100,
                Duration.ZERO
        );

        List<StockBasicDto> stocks = sourceService.fetchAll();

        assertEquals(3, stocks.size());
        assertEquals(List.of("600000", "000001", "920001"),
                stocks.stream().map(StockBasicDto::getStockCode).toList());
        server.verify();
    }

    @Test
    void shouldFetchAllPagesWhenStockCountExceedsPageSize() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("count?node=hs_a")))
                .andRespond(withSuccess("3", MediaType.TEXT_PLAIN));
        server.expect(requestTo(containsString("list?page=1")))
                .andRespond(withSuccess(
                        """
                                [
                                  {symbol:"sh600000",code:"600000",name:"浦发银行"},
                                  {symbol:"sz000001",code:"000001",name:"平安银行"}
                                ]
                                """,
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(allOf(
                        containsString("list?page=2"),
                        containsString("num=2")
                )))
                .andRespond(withSuccess(
                        "[{symbol:\"bj920001\",code:\"920001\",name:\"北交样本\"}]",
                        MediaType.APPLICATION_JSON
                ));
        SinaStockBasicSourceServiceImpl sourceService = new SinaStockBasicSourceServiceImpl(
                restTemplate,
                COUNT_URL,
                LIST_URL,
                2,
                Duration.ZERO
        );

        assertEquals(3, sourceService.fetchAll().size());
        server.verify();
    }

    @Test
    void shouldRejectIncompletePage() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("count?node=hs_a")))
                .andRespond(withSuccess("3", MediaType.TEXT_PLAIN));
        server.expect(requestTo(containsString("list?page=1")))
                .andRespond(withSuccess(
                        "[{symbol:\"sh600000\",code:\"600000\",name:\"浦发银行\"}]",
                        MediaType.APPLICATION_JSON
                ));
        SinaStockBasicSourceServiceImpl sourceService = new SinaStockBasicSourceServiceImpl(
                restTemplate,
                COUNT_URL,
                LIST_URL,
                2,
                Duration.ZERO
        );

        assertThrows(IllegalStateException.class, sourceService::fetchAll);
        server.verify();
    }

    @Test
    void shouldRejectMalformedStockData() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("count?node=hs_a")))
                .andRespond(withSuccess("1", MediaType.TEXT_PLAIN));
        server.expect(requestTo(containsString("list?page=1")))
                .andRespond(withSuccess(
                        "[{symbol:\"sh600000\",code:\"600000\",name:\"\"}]",
                        MediaType.APPLICATION_JSON
                ));
        SinaStockBasicSourceServiceImpl sourceService = new SinaStockBasicSourceServiceImpl(
                restTemplate,
                COUNT_URL,
                LIST_URL,
                100,
                Duration.ZERO
        );

        assertThrows(IllegalStateException.class, sourceService::fetchAll);
        server.verify();
    }
}
