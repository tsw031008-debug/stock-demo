package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexStyleCalculator;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexStyleServiceImplTest {

    @Mock
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private IndexStyleCalculator indexStyleCalculator;
    @Mock
    private IndexDailyQuoteSourceService sourceService;

    @Test
    void shouldIncludeTodayAndPreviousFourTradingDaysAfterMarketOpen() {
        LocalDate currentDate = LocalDate.of(2026, 9, 7);
        List<LocalDate> tradeDates = List.of(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 4),
                currentDate
        );
        List<LocalDate> storedTradeDates = tradeDates.subList(0, 4);
        List<IndexDailyQuote> storedQuotes = List.of(quote(tradeDates.get(0)));
        List<IndexDailyQuote> currentQuotes = List.of(quote(currentDate));
        List<IndexDailyQuote> allQuotes = new ArrayList<>(storedQuotes);
        allQuotes.addAll(currentQuotes);
        IndexStyleServiceImpl indexStyleService = createService("2026-09-07T02:00:00Z");
        when(indexDailyQuoteMapper.selectLatestTradeDate())
                .thenReturn(LocalDate.of(2026, 9, 4));
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 4))
                .thenReturn(tradeDates.get(0));
        when(tradeCalendarService.getTradingDays(tradeDates.get(0), currentDate))
                .thenReturn(tradeDates);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(storedTradeDates, indexCodes()))
                .thenReturn(storedQuotes);
        when(sourceService.fetch(currentDate)).thenReturn(currentQuotes);
        when(indexStyleCalculator.calculate(tradeDates, allQuotes)).thenReturn(List.of());

        IndexStyleComparisonDto result = indexStyleService.getLatest();

        assertEquals(currentDate, result.getStatisticsDate());
        assertEquals(tradeDates, result.getTradeDates());
        verify(indexStyleCalculator).calculate(tradeDates, allQuotes);
    }

    @Test
    void shouldUseLatestStoredFiveDaysBeforeMarketOpen() {
        LocalDate currentDate = LocalDate.of(2026, 9, 7);
        LocalDate statisticsDate = LocalDate.of(2026, 9, 4);
        List<LocalDate> tradeDates = storedTradeDates(statisticsDate);
        List<IndexDailyQuote> quotes = List.of();
        IndexStyleServiceImpl indexStyleService = createService("2026-09-07T00:30:00Z");
        stubStoredWindow(currentDate, statisticsDate, tradeDates, quotes, true);

        IndexStyleComparisonDto result = indexStyleService.getLatest();

        assertEquals(statisticsDate, result.getStatisticsDate());
        verify(sourceService, never()).fetch(currentDate);
    }

    @Test
    void shouldUseLatestStoredFiveDaysOnNonTradingDay() {
        LocalDate currentDate = LocalDate.of(2026, 9, 6);
        LocalDate statisticsDate = LocalDate.of(2026, 9, 4);
        List<LocalDate> tradeDates = storedTradeDates(statisticsDate);
        List<IndexDailyQuote> quotes = List.of();
        IndexStyleServiceImpl indexStyleService = createService("2026-09-06T02:00:00Z");
        stubStoredWindow(currentDate, statisticsDate, tradeDates, quotes, false);

        IndexStyleComparisonDto result = indexStyleService.getLatest();

        assertEquals(statisticsDate, result.getStatisticsDate());
        verify(sourceService, never()).fetch(currentDate);
    }

    @Test
    void shouldUseTodayStoredFiveDaysWithoutRequestingTencentAgain() {
        LocalDate currentDate = LocalDate.of(2026, 9, 7);
        List<LocalDate> tradeDates = List.of(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 4),
                currentDate
        );
        List<IndexDailyQuote> quotes = List.of();
        IndexStyleServiceImpl indexStyleService = createService("2026-09-07T07:30:00Z");
        when(indexDailyQuoteMapper.selectLatestTradeDate()).thenReturn(currentDate);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 4))
                .thenReturn(tradeDates.get(0));
        when(tradeCalendarService.getTradingDays(tradeDates.get(0), currentDate))
                .thenReturn(tradeDates);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(tradeDates, indexCodes()))
                .thenReturn(quotes);
        when(indexStyleCalculator.calculate(tradeDates, quotes)).thenReturn(List.of());

        IndexStyleComparisonDto result = indexStyleService.getLatest();

        assertEquals(currentDate, result.getStatisticsDate());
        verify(sourceService, never()).fetch(currentDate);
    }

    private void stubStoredWindow(
            LocalDate currentDate,
            LocalDate statisticsDate,
            List<LocalDate> tradeDates,
            List<IndexDailyQuote> quotes,
            boolean tradingDay
    ) {
        when(indexDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(tradingDay);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 4))
                .thenReturn(tradeDates.get(0));
        when(tradeCalendarService.getTradingDays(tradeDates.get(0), statisticsDate))
                .thenReturn(tradeDates);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(tradeDates, indexCodes()))
                .thenReturn(quotes);
        when(indexStyleCalculator.calculate(tradeDates, quotes)).thenReturn(List.of());
    }

    private IndexStyleServiceImpl createService(String instant) {
        return new IndexStyleServiceImpl(
                indexDailyQuoteMapper,
                tradeCalendarService,
                indexStyleCalculator,
                sourceService,
                Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai"))
        );
    }

    private List<String> indexCodes() {
        return Arrays.stream(IndexStyleIndex.values())
                .map(IndexStyleIndex::getIndexCode)
                .toList();
    }

    private List<LocalDate> storedTradeDates(LocalDate statisticsDate) {
        return List.of(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
                statisticsDate
        );
    }

    private IndexDailyQuote quote(LocalDate tradeDate) {
        return IndexDailyQuote.builder()
                .indexCode(IndexStyleIndex.SSE_50.getIndexCode())
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal("100"))
                .previousClosePrice(new BigDecimal("99"))
                .build();
    }
}
