package cn.djct.stockdemo.service.stockdailyquote;

import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockdailyquote.impl.StockDailyQuoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDailyQuoteServiceTest {

    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;

    @InjectMocks
    private StockDailyQuoteServiceImpl stockDailyQuoteService;

    @Test
    void shouldUpsertQuotesInBatchesOfFiveHundred() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int index = 0; index < 1001; index++) {
            quotes.add(closingQuote(String.format("%06d", index), tradeDate));
        }
        when(stockDailyQuoteMapper.countByTradeDate(tradeDate)).thenReturn(1001);

        assertEquals(1001, stockDailyQuoteService.saveSnapshot(quotes));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockDailyQuote>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockDailyQuoteMapper, times(3)).upsertBatch(batchCaptor.capture());
        assertEquals(List.of(500, 500, 1),
                batchCaptor.getAllValues().stream().map(List::size).toList());
    }

    @Test
    void shouldRejectIntradayQuoteBeforeWriting() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        StockDailyQuote quote = closingQuote("600000", date);
        quote.setQuoteTime(date.atTime(10, 0));
        assertThrows(IllegalStateException.class, () -> stockDailyQuoteService.saveSnapshot(List.of(quote)));
        verifyNoInteractions(stockDailyQuoteMapper);
    }

    @Test
    void shouldRejectPartialStaleAndWrongStockSnapshots() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        StockDailyQuote quote = closingQuote("600000", date);
        when(stockDailyQuoteMapper.selectByStockCodesAndTradeDates(List.of("600000"), List.of(date)))
                .thenReturn(List.of(quote));
        assertTrue(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
        quote.setDataStatus("PARTIAL");
        assertFalse(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
        quote.setDataStatus("COMPLETE");
        quote.setQuoteTime(date.atTime(10, 0));
        assertFalse(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
        quote.setQuoteTime(date.atTime(15, 0));
        quote.setStockCode("600001");
        assertFalse(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
    }

    @Test
    void shouldAcceptSuspendedQuoteOnlyWhenCollectedAfterClose() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        StockDailyQuote quote = closingQuote("600000", date);
        quote.setClosePrice(BigDecimal.ZERO);
        quote.setTurnoverAmountYuan(BigDecimal.ZERO);
        quote.setQuoteTime(date.minusDays(1).atTime(15, 0));
        quote.setDataStatus("PARTIAL");
        quote.setCollectedAt(date.atTime(15, 2));
        when(stockDailyQuoteMapper.selectByStockCodesAndTradeDates(List.of("600000"), List.of(date)))
                .thenReturn(List.of(quote));
        assertTrue(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
        quote.setCollectedAt(date.atTime(10, 0));
        assertFalse(stockDailyQuoteService.hasClosingQuotes(date, List.of("600000")));
    }

    private StockDailyQuote closingQuote(String code, LocalDate date) {
        return StockDailyQuote.builder().stockCode(code).tradeDate(date).dataSource("TENCENT")
                .dataStatus("COMPLETE").quoteTime(date.atTime(15, 0)).collectedAt(date.atTime(15, 2))
                .closePrice(BigDecimal.TEN).previousClosePrice(BigDecimal.TEN).openPrice(BigDecimal.TEN)
                .highPrice(BigDecimal.TEN).lowPrice(BigDecimal.TEN).changePercent(BigDecimal.ZERO)
                .turnoverAmountYuan(BigDecimal.TEN).build();
    }
}
