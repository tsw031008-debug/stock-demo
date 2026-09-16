package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.StockIndexDifferenceCalculator;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockIndexDifferenceServiceImplTest {
    private final StockDailyQuoteMapper stockDailyQuoteMapper = mock(StockDailyQuoteMapper.class);
    private final IndexDailyQuoteMapper indexDailyQuoteMapper = mock(IndexDailyQuoteMapper.class);
    private final TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
    private final StockIndexDifferenceCalculator stockIndexDifferenceCalculator = new StockIndexDifferenceCalculator();

    @ParameterizedTest
    @CsvSource({"2026-09-14,2026-09-11", "2026-09-01,2026-08-31", "2026-01-05,2025-12-31"})
    void shouldUseCalendarPreviousDateAndOnlyCsi300(String current, String previous) {
        LocalDate date = LocalDate.parse(current);
        LocalDate previousDate = LocalDate.parse(previous);
        List<LocalDate> dates = List.of(date, previousDate);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(date, 1)).thenReturn(previousDate);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(dates, List.of("000300")))
                .thenReturn(List.of(index(date), index(previousDate)));
        when(stockDailyQuoteMapper.selectClosePricesByTradeDates(dates)).thenReturn(List.of(
                new StockClosePriceDto("000001", date, new BigDecimal("108")),
                new StockClosePriceDto("000001", previousDate, new BigDecimal("100"))));
        var result = service("2026-09-15T07:00:00Z").findByTradeDate(date);
        assertEquals(previousDate, result.getPreviousTradeDate());
        assertEquals(1, result.getStrong().get(3).getCount());
        assertEquals("000300", result.getIndexCode());
        verify(stockDailyQuoteMapper, never()).selectLatestTradeDate();
    }

    @Test
    void shouldRejectMissingAndFutureDateBeforeQueryingData() {
        var stockIndexDifferenceService = service("2026-09-15T07:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> stockIndexDifferenceService.findByTradeDate(null));
        assertThrows(IllegalArgumentException.class,
                () -> stockIndexDifferenceService.findByTradeDate(LocalDate.of(2026, 9, 16)));
        verifyNoInteractions(indexDailyQuoteMapper, stockDailyQuoteMapper, tradeCalendarService);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"2026-09-13", "2026-01-01"})
    void shouldRejectNonTradingDate(String value) {
        assertThrows(IllegalArgumentException.class,
                () -> service("2026-09-15T07:00:00Z").findByTradeDate(LocalDate.parse(value)));
        verifyNoInteractions(indexDailyQuoteMapper, stockDailyQuoteMapper);
    }

    @Test
    void shouldRejectBeforeCloseAndCheckDataAtClose() {
        LocalDate date = LocalDate.of(2026, 9, 15);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service("2026-09-15T06:59:59Z").findByTradeDate(date));
        verifyNoInteractions(indexDailyQuoteMapper, stockDailyQuoteMapper);
        when(tradeCalendarService.getPreviousTradingDay(date, 1)).thenReturn(date.minusDays(1));
        assertThrows(IllegalStateException.class,
                () -> service("2026-09-15T07:00:00Z").findByTradeDate(date));
        verify(indexDailyQuoteMapper).selectByTradeDatesAndCodes(List.of(date, date.minusDays(1)), List.of("000300"));
    }

    @Test
    void shouldRejectMissingOrInvalidIndexBeforeReadingStocks() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        LocalDate previous = LocalDate.of(2026, 9, 11);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(date, 1)).thenReturn(previous);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(List.of(date, previous), List.of("000300")))
                .thenReturn(List.of(index(date)));
        assertThrows(IllegalStateException.class, () -> service("2026-09-15T07:00:00Z").findByTradeDate(date));
        IndexDailyQuote invalid = index(previous);
        invalid.setClosePrice(BigDecimal.ZERO);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(List.of(date, previous), List.of("000300")))
                .thenReturn(List.of(index(date), invalid));
        assertThrows(IllegalStateException.class, () -> service("2026-09-15T07:00:00Z").findByTradeDate(date));
        verifyNoInteractions(stockDailyQuoteMapper);
    }

    private IndexDailyQuote index(LocalDate date) {
        return IndexDailyQuote.builder().indexCode("000300").tradeDate(date)
                .closePrice(new BigDecimal("100")).build();
    }

    private StockIndexDifferenceServiceImpl service(String instant) {
        return new StockIndexDifferenceServiceImpl(stockDailyQuoteMapper, indexDailyQuoteMapper,
                tradeCalendarService, stockIndexDifferenceCalculator,
                Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai")));
    }
}
