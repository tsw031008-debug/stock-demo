package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexMinuteQuoteSyncServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 27);

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private IndexMinuteQuoteSourceService indexMinuteQuoteSourceService;

    @Mock
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;

    @InjectMocks
    private IndexMinuteQuoteSyncServiceImpl indexMinuteQuoteSyncService;

    @ParameterizedTest
    @MethodSource("validCollectionTimes")
    void shouldSaveValidCollectionMinute(LocalTime time) {
        LocalDateTime triggerTime = LocalDateTime.of(TRADE_DATE, time).withSecond(10);
        IndexMinuteQuote quote = quote(triggerTime.withSecond(45));
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteSourceService.fetchShanghaiComposite()).thenReturn(quote);
        when(indexMinuteQuoteMapper.upsert(quote)).thenReturn(1);

        assertEquals(1, indexMinuteQuoteSyncService.synchronize(triggerTime));

        assertEquals(triggerTime.withSecond(0), quote.getQuoteTime());
        verify(indexMinuteQuoteMapper).upsert(quote);
    }

    @ParameterizedTest
    @MethodSource("invalidCollectionTimes")
    void shouldSkipInvalidCollectionMinute(LocalTime time) {
        LocalDateTime triggerTime = LocalDateTime.of(TRADE_DATE, time);

        assertEquals(0, indexMinuteQuoteSyncService.synchronize(triggerTime));

        verify(tradeCalendarService, never()).isTradingDay(TRADE_DATE);
        verify(indexMinuteQuoteSourceService, never()).fetchShanghaiComposite();
        verify(indexMinuteQuoteMapper, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipNonTradingDay() {
        LocalDateTime triggerTime = LocalDateTime.of(2026, 8, 29, 9, 31);
        when(tradeCalendarService.isTradingDay(triggerTime.toLocalDate())).thenReturn(false);

        assertEquals(0, indexMinuteQuoteSyncService.synchronize(triggerTime));

        verify(indexMinuteQuoteSourceService, never()).fetchShanghaiComposite();
    }

    @Test
    void shouldRejectQuoteFromAnotherMinute() {
        LocalDateTime triggerTime = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 32, 10));
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteSourceService.fetchShanghaiComposite())
                .thenReturn(quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31, 59))));

        assertThrows(
                IllegalStateException.class,
                () -> indexMinuteQuoteSyncService.synchronize(triggerTime)
        );

        verify(indexMinuteQuoteMapper, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldNotRequestSourceWhenMinutesAreComplete() {
        LocalDateTime checkTime = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 32));
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)),
                checkTime
        )).thenReturn(List.of(
                quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31))),
                quote(checkTime)
        ));

        assertEquals(0, indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime));

        verify(indexMinuteQuoteSourceService, never()).fetchShanghaiCompositeMinutes();
        verify(indexMinuteQuoteMapper, never()).upsertBatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldOnlySaveMissingMinutes() {
        LocalDateTime checkTime = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 32));
        IndexMinuteQuote missingQuote = quote(checkTime);
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)),
                checkTime
        )).thenReturn(List.of(quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)))));
        when(indexMinuteQuoteSourceService.fetchShanghaiCompositeMinutes()).thenReturn(List.of(
                quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31))),
                missingQuote
        ));
        when(indexMinuteQuoteMapper.upsertBatch(List.of(missingQuote))).thenReturn(1);

        assertEquals(1, indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime));

        verify(indexMinuteQuoteMapper).upsertBatch(List.of(missingQuote));
    }

    @Test
    void shouldRejectIncompleteRecoverySourceWithoutWriting() {
        LocalDateTime checkTime = LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 32));
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)),
                checkTime
        )).thenReturn(List.of(quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)))));
        when(indexMinuteQuoteSourceService.fetchShanghaiCompositeMinutes()).thenReturn(
                List.of(quote(LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31))))
        );

        assertThrows(
                IllegalStateException.class,
                () -> indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime)
        );

        verify(indexMinuteQuoteMapper, never()).upsertBatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldCheckAllTradingMinutesAfterClose() {
        LocalDateTime checkTime = LocalDateTime.of(TRADE_DATE, LocalTime.of(15, 2));
        List<IndexMinuteQuote> completeQuotes = completeDayQuotes();
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(TRADE_DATE, LocalTime.of(9, 31)),
                LocalDateTime.of(TRADE_DATE, LocalTime.of(15, 0))
        )).thenReturn(completeQuotes);

        assertEquals(0, indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime));
        assertEquals(240, completeQuotes.size());

        verify(indexMinuteQuoteSourceService, never()).fetchShanghaiCompositeMinutes();
    }

    private static Stream<LocalTime> validCollectionTimes() {
        return Stream.of(
                LocalTime.of(9, 31),
                LocalTime.of(11, 30),
                LocalTime.of(13, 1),
                LocalTime.of(15, 0)
        );
    }

    private static Stream<LocalTime> invalidCollectionTimes() {
        return Stream.of(
                LocalTime.of(9, 30),
                LocalTime.of(11, 31),
                LocalTime.of(13, 0),
                LocalTime.of(15, 1)
        );
    }

    private IndexMinuteQuote quote(LocalDateTime quoteTime) {
        return IndexMinuteQuote.builder()
                .indexCode("000001")
                .indexName("上证指数")
                .tradeDate(quoteTime.toLocalDate())
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal("3850.12"))
                .previousClosePrice(new BigDecimal("3820.10"))
                .dataSource("TENCENT")
                .collectedAt(quoteTime)
                .build();
    }

    private List<IndexMinuteQuote> completeDayQuotes() {
        List<IndexMinuteQuote> result = new ArrayList<>(240);
        appendQuotes(result, LocalTime.of(9, 31), LocalTime.of(11, 30));
        appendQuotes(result, LocalTime.of(13, 1), LocalTime.of(15, 0));
        return result;
    }

    private void appendQuotes(
            List<IndexMinuteQuote> result,
            LocalTime startTime,
            LocalTime endTime
    ) {
        LocalDateTime current = LocalDateTime.of(TRADE_DATE, startTime);
        LocalDateTime end = LocalDateTime.of(TRADE_DATE, endTime);
        while (!current.isAfter(end)) {
            result.add(quote(current));
            current = current.plusMinutes(1);
        }
    }
}
