package cn.djct.stockdemo.service;

import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.impl.StockDailyQuoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            quotes.add(StockDailyQuote.builder()
                    .stockCode(String.format("%06d", index))
                    .tradeDate(tradeDate)
                    .build());
        }
        when(stockDailyQuoteMapper.countByTradeDate(tradeDate)).thenReturn(1001);

        assertEquals(1001, stockDailyQuoteService.saveSnapshot(quotes));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockDailyQuote>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockDailyQuoteMapper, times(3)).upsertBatch(batchCaptor.capture());
        assertEquals(List.of(500, 500, 1),
                batchCaptor.getAllValues().stream().map(List::size).toList());
    }
}
