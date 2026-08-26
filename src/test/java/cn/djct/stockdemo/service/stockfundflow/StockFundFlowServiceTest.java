package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockFundFlowDto;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.stockfundflow.impl.StockFundFlowServiceImpl;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockFundFlowServiceTest {

    @Test
    void shouldRejectMixedTradeDates() {
        StockFundFlowMapper mapper = mock(StockFundFlowMapper.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        StockFundFlowService service = new StockFundFlowServiceImpl(mapper, tradeCalendarService);
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

    @Test
    void shouldQueryTradingDayFundFlowsByPage() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 25);
        StockFundFlowMapper mapper = mock(StockFundFlowMapper.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        StockFundFlowService service = new StockFundFlowServiceImpl(mapper, tradeCalendarService);
        StockFundFlowDto record = StockFundFlowDto.builder()
                .tradeDate(tradeDate)
                .stockCode("600519")
                .mainNetInflowYuan(new BigDecimal("100.00"))
                .build();
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(mapper.countByTradeDate(tradeDate)).thenReturn(5211);
        when(mapper.selectPageByTradeDate(tradeDate, 20, 20)).thenReturn(List.of(record));

        PageDto<StockFundFlowDto> page = service.findByTradeDate(tradeDate, 2, 20);

        assertEquals(2, page.getPageNum());
        assertEquals(20, page.getPageSize());
        assertEquals(5211, page.getTotal());
        assertEquals(List.of(record), page.getRecords());
        verify(mapper).selectPageByTradeDate(tradeDate, 20, 20);
    }

    @Test
    void shouldRejectNonTradingDateBeforeQueryingDatabase() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 23);
        StockFundFlowMapper mapper = mock(StockFundFlowMapper.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        StockFundFlowService service = new StockFundFlowServiceImpl(mapper, tradeCalendarService);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findByTradeDate(tradeDate, 1, 20)
        );

        assertEquals("查询日期不是交易日", exception.getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldReturnEmptyPageWhenTradingDayHasNoData() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 24);
        StockFundFlowMapper mapper = mock(StockFundFlowMapper.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        StockFundFlowService service = new StockFundFlowServiceImpl(mapper, tradeCalendarService);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(mapper.countByTradeDate(tradeDate)).thenReturn(0);

        PageDto<StockFundFlowDto> page = service.findByTradeDate(tradeDate, 1, 20);

        assertEquals(0, page.getTotal());
        assertEquals(List.of(), page.getRecords());
    }
}
