package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexDailyQuoteSyncServiceImplTest {

    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private IndexDailyQuoteSourceService sourceService;
    @Mock
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
    @InjectMocks
    private IndexDailyQuoteSyncServiceImpl syncService;

    @Test
    void shouldSaveFiveCompleteIndexQuotes() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 3);
        List<IndexDailyQuote> quotes = Arrays.stream(IndexStyleIndex.values())
                .map(index -> quote(index, tradeDate))
                .toList();
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(sourceService.fetch(tradeDate)).thenReturn(quotes);

        assertEquals(5, syncService.synchronize(tradeDate));
        verify(indexDailyQuoteMapper).upsertBatch(quotes);
    }

    @Test
    void shouldSkipNonTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 5);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(false);

        assertEquals(0, syncService.synchronize(tradeDate));
        verify(sourceService, never()).fetch(tradeDate);
    }

    @Test
    void shouldRejectBatchWithoutCsi300() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 14);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(sourceService.fetch(tradeDate)).thenReturn(Arrays.stream(IndexStyleIndex.styleValues())
                .map(index -> quote(index, tradeDate)).toList());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> syncService.synchronize(tradeDate));
        org.mockito.Mockito.verifyNoInteractions(indexDailyQuoteMapper);
    }

    private IndexDailyQuote quote(IndexStyleIndex index, LocalDate tradeDate) {
        return IndexDailyQuote.builder()
                .indexCode(index.getIndexCode())
                .indexName(index.getIndexName())
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal("100"))
                .previousClosePrice(new BigDecimal("99"))
                .build();
    }
}
