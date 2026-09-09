package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.TechnologyStockTurnoverCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.vo.TechnologyStockTurnoverRespVo;
import cn.djct.stockdemo.service.stockalert.TechnologyStockPoolSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TechnologyStockTurnoverServiceImplTest {
    private final StockDailyQuoteMapper mapper = mock(StockDailyQuoteMapper.class);
    private final TradeCalendarService calendar = mock(TradeCalendarService.class);
    private final TechnologyStockPoolSourceService source = mock(TechnologyStockPoolSourceService.class);
    private final TechnologyStockTurnoverCalculator calculator = mock(TechnologyStockTurnoverCalculator.class);
    private final TechnologyStockTurnoverServiceImpl service =
            new TechnologyStockTurnoverServiceImpl(mapper, calendar, source, calculator);
    private final LocalDate date = LocalDate.of(2026, 1, 5);

    @Test
    void shouldUseRequestedDateAndCalendarWithoutCountThreshold() {
        var dates = prepare();
        var result = TechnologyStockTurnoverRespVo.builder().statisticsDate(date).build();
        when(source.fetchStockCodes()).thenReturn(List.of("000001"));
        when(mapper.selectTechnologyStockQuotes(List.of("000001"), dates)).thenReturn(List.of());
        when(calculator.calculate(dates, List.of())).thenReturn(result);
        assertSame(result, service.findByTradeDate(date));
        verify(mapper).selectTechnologyStockQuotes(List.of("000001"), dates);
        verify(mapper, never()).selectLatestTradeDate();
    }

    @Test
    void shouldRejectNullFutureAndNonTradingDate() {
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(null));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(
                LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(date));
        verifyNoInteractions(mapper, source);
    }

    @Test
    void shouldReportMissingDayWithoutFallbackOrFetchingSource() {
        when(calendar.isTradingDay(date)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(date));
        verifyNoInteractions(source, calculator);
        verify(mapper, never()).selectLatestTradeDate();
    }

    @Test
    void shouldRejectEmptyPoolAndPropagateSourceFailure() {
        prepare();
        when(source.fetchStockCodes()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(date));
        when(source.fetchStockCodes()).thenThrow(new IllegalStateException("数据源失败"));
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(date));
        verifyNoInteractions(calculator);
    }

    private List<LocalDate> prepare() {
        var dates = List.of(date, LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 12, 30), LocalDate.of(2025, 12, 29));
        when(calendar.isTradingDay(date)).thenReturn(true);
        when(mapper.countByTradeDate(date)).thenReturn(1);
        for (int i = 1; i <= 3; i++) when(calendar.getPreviousTradingDay(date, i)).thenReturn(dates.get(i));
        return dates;
    }
}
