package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.stockfundflow.impl.StockFundFlowServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class StockFundFlowServiceTest {

    @Test
    void shouldRejectMixedTradeDates() {
        StockFundFlowMapper mapper = mock(StockFundFlowMapper.class);
        StockFundFlowService service = new StockFundFlowServiceImpl(mapper);
        List<StockFundFlow> fundFlows = List.of(
                StockFundFlow.builder()
                        .stockCode("600000")
                        .tradeDate(LocalDate.of(2026, 8, 20))
                        .build(),
                StockFundFlow.builder()
                        .stockCode("000001")
                        .tradeDate(LocalDate.of(2026, 8, 21))
                        .build()
        );

        assertThrows(IllegalArgumentException.class, () -> service.saveSnapshot(fundFlows));
    }
}
