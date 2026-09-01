package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.common.IndexDivergenceSignalCalculator;
import cn.djct.stockdemo.common.IndexMacdCalculator;
import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexDivergenceSignalDto;
import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexDivergenceSignalServiceImplTest {

    private static final LocalDate PREVIOUS_TRADE_DATE = LocalDate.of(2026, 8, 27);
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 28);
    private static final LocalDateTime CURRENT_MINUTE = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 35));

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;

    @Mock
    private IndexDivergenceSignalMapper indexDivergenceSignalMapper;

    @Mock
    private IndexMacdCalculator indexMacdCalculator;

    @Mock
    private IndexDivergenceSignalCalculator indexDivergenceSignalCalculator;

    @InjectMocks
    private IndexDivergenceSignalServiceImpl service;

    @Test
    void shouldSkipWhenPreviousTradingDayIsIncomplete() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.remove(quotes.size() - 1);
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);

        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));

        verify(indexMacdCalculator, never()).calculate(anyList());
        verify(indexDivergenceSignalMapper, never()).upsertBatch(anyList());
    }

    @Test
    void shouldReuseIncompletePreviousTradingDayCheck() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.remove(quotes.size() - 1);
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);

        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));
        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));

        verify(tradeCalendarService, times(1)).getPreviousTradingDay(TRADE_DATE, 1);
        verify(indexMinuteQuoteMapper, times(1)).selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        );
    }

    @Test
    void shouldSkipWhenPreviousTradingDayHasMinuteGap() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.get(100).setQuoteTime(LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(12, 0)));
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);

        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));

        verify(indexMacdCalculator, never()).calculate(anyList());
        verify(indexDivergenceSignalMapper, never()).upsertBatch(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCalculateCrossDayMacdAndSaveCurrentDaySignal() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.addAll(currentDayQuotes());
        List<IndexMacdDto> macdItems = List.of(macdItem(CURRENT_MINUTE));
        IndexDivergenceSignalDto signal = signal(CURRENT_MINUTE);
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);
        when(indexMacdCalculator.calculate(quotes)).thenReturn(macdItems);
        when(indexDivergenceSignalCalculator.detect(macdItems)).thenReturn(List.of(signal));
        when(indexDivergenceSignalMapper.upsertBatch(anyList())).thenReturn(1);

        assertEquals(1, service.calculateAndSave(CURRENT_MINUTE.plusSeconds(10)));

        ArgumentCaptor<List<IndexDivergenceSignal>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexDivergenceSignalMapper).upsertBatch(captor.capture());
        IndexDivergenceSignal saved = captor.getValue().get(0);
        assertEquals("000001", saved.getIndexCode());
        assertEquals(IndexDivergenceSignalType.MACD_BOTTOM, saved.getSignalType());
        assertEquals(CURRENT_MINUTE, saved.getSignalTime());
    }

    @Test
    void shouldSkipWhenCurrentTradingDayHasMinuteGap() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        List<IndexMinuteQuote> currentDayQuotes = currentDayQuotes();
        currentDayQuotes.remove(2);
        quotes.addAll(currentDayQuotes);
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);

        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));

        verify(indexMacdCalculator, never()).calculate(anyList());
        verify(indexDivergenceSignalMapper, never()).upsertBatch(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveOnlySignalConfirmedAtCurrentMinute() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.addAll(currentDayQuotes());
        List<IndexMacdDto> macdItems = List.of(macdItem(CURRENT_MINUTE));
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);
        when(indexMacdCalculator.calculate(quotes)).thenReturn(macdItems);
        when(indexDivergenceSignalCalculator.detect(macdItems)).thenReturn(List.of(
                signal(CURRENT_MINUTE.minusMinutes(1)),
                signal(CURRENT_MINUTE)
        ));
        when(indexDivergenceSignalMapper.upsertBatch(anyList())).thenReturn(1);

        assertEquals(1, service.calculateAndSave(CURRENT_MINUTE));

        ArgumentCaptor<List<IndexDivergenceSignal>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexDivergenceSignalMapper).upsertBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(CURRENT_MINUTE, captor.getValue().get(0).getSignalTime());
    }

    @Test
    void shouldNotSaveHistoricalSignalFromWarmupDay() {
        List<IndexMinuteQuote> quotes = completePreviousDayQuotes();
        quotes.addAll(currentDayQuotes());
        List<IndexMacdDto> macdItems = List.of(macdItem(CURRENT_MINUTE));
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(PREVIOUS_TRADE_DATE);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31)),
                CURRENT_MINUTE
        )).thenReturn(quotes);
        when(indexMacdCalculator.calculate(quotes)).thenReturn(macdItems);
        when(indexDivergenceSignalCalculator.detect(macdItems))
                .thenReturn(List.of(signal(LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(14, 0)))));

        assertEquals(0, service.calculateAndSave(CURRENT_MINUTE));

        verify(indexDivergenceSignalMapper, never()).upsertBatch(anyList());
    }

    private List<IndexMinuteQuote> completePreviousDayQuotes() {
        List<IndexMinuteQuote> quotes = new ArrayList<>(240);
        LocalDateTime morning = LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(9, 31));
        for (int index = 0; index < 120; index++) {
            quotes.add(quote(morning.plusMinutes(index)));
        }
        LocalDateTime afternoon = LocalDateTime.of(PREVIOUS_TRADE_DATE, LocalTime.of(13, 1));
        for (int index = 0; index < 120; index++) {
            quotes.add(quote(afternoon.plusMinutes(index)));
        }
        return quotes;
    }

    private List<IndexMinuteQuote> currentDayQuotes() {
        List<IndexMinuteQuote> quotes = new ArrayList<>();
        LocalDateTime current = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31));
        while (!current.isAfter(CURRENT_MINUTE)) {
            quotes.add(quote(current));
            current = current.plusMinutes(1);
        }
        return quotes;
    }

    private IndexMinuteQuote quote(LocalDateTime quoteTime) {
        return IndexMinuteQuote.builder()
                .indexCode("000001")
                .tradeDate(quoteTime.toLocalDate())
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal("3850.12"))
                .build();
    }

    private IndexMacdDto macdItem(LocalDateTime quoteTime) {
        return IndexMacdDto.builder()
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal("3850.12"))
                .dif(new BigDecimal("1.2"))
                .dea(new BigDecimal("1.1"))
                .macd(new BigDecimal("0.2"))
                .build();
    }

    private IndexDivergenceSignalDto signal(LocalDateTime signalTime) {
        return IndexDivergenceSignalDto.builder()
                .signalType(IndexDivergenceSignalType.MACD_BOTTOM)
                .signalTime(signalTime)
                .previousIntervalStartTime(signalTime.minusMinutes(20))
                .previousIntervalEndTime(signalTime.minusMinutes(15))
                .currentIntervalStartTime(signalTime.minusMinutes(5))
                .currentIntervalEndTime(signalTime)
                .previousPriceExtreme(new BigDecimal("3860.00"))
                .currentPriceExtreme(new BigDecimal("3850.00"))
                .previousMacdExtreme(new BigDecimal("-2.00"))
                .currentMacdExtreme(new BigDecimal("-1.00"))
                .previousDifExtreme(new BigDecimal("-1.50"))
                .currentDifExtreme(new BigDecimal("-0.80"))
                .build();
    }
}
