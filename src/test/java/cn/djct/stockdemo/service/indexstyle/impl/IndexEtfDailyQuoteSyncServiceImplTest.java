package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSourceService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexEtfDailyQuoteSyncServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 4);

    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private IndexEtfDailyQuoteSourceService sourceService;
    @Mock
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;
    @InjectMocks
    private IndexEtfDailyQuoteSyncServiceImpl syncService;

    @Test
    void shouldSkipExistingCompleteQuotesWithoutRequestingTencent() {
        List<IndexEtfDailyQuote> quotes = completeQuotes();
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(TRADE_DATE),
                etfCodes()
        )).thenReturn(quotes);

        assertEquals(0, syncService.synchronize(TRADE_DATE));

        verify(sourceService, never()).fetch(TRADE_DATE);
    }

    @Test
    void shouldSaveCompleteTencentQuotes() {
        List<IndexEtfDailyQuote> quotes = completeQuotes();
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(TRADE_DATE),
                etfCodes()
        )).thenReturn(List.of());
        when(sourceService.fetch(TRADE_DATE)).thenReturn(quotes);

        assertEquals(4, syncService.synchronize(TRADE_DATE));

        verify(indexEtfDailyQuoteMapper).upsertBatch(quotes);
    }

    @Test
    void shouldRejectIncompleteTencentQuotes() {
        List<IndexEtfDailyQuote> quotes = completeQuotes().subList(0, 3);
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(TRADE_DATE),
                etfCodes()
        )).thenReturn(List.of());
        when(sourceService.fetch(TRADE_DATE)).thenReturn(quotes);

        assertThrows(IllegalStateException.class, () -> syncService.synchronize(TRADE_DATE));

        verify(indexEtfDailyQuoteMapper, never()).upsertBatch(quotes);
    }

    private List<String> etfCodes() {
        return Arrays.stream(IndexEtf.values()).map(IndexEtf::getEtfCode).toList();
    }

    private List<IndexEtfDailyQuote> completeQuotes() {
        return Arrays.stream(IndexEtf.values())
                .map(etf -> IndexEtfDailyQuote.builder()
                        .etfCode(etf.getEtfCode())
                        .indexName(etf.getIndexName())
                        .tradeDate(TRADE_DATE)
                        .closePrice(BigDecimal.ONE)
                        .build())
                .toList();
    }
}
