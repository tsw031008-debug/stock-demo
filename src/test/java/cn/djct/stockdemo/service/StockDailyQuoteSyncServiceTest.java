package cn.djct.stockdemo.service;

import cn.djct.stockdemo.face.StockDailyQuoteCollectionFace;
import cn.djct.stockdemo.service.impl.StockDailyQuoteSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDailyQuoteSyncServiceTest {

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace;

    @InjectMocks
    private StockDailyQuoteSyncServiceImpl stockDailyQuoteSyncService;

    @Test
    void shouldSkipNonTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 22);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(false);

        assertEquals(0, stockDailyQuoteSyncService.synchronize(tradeDate));

        verify(stockDailyQuoteCollectionFace, never()).synchronize(tradeDate);
    }

    @Test
    void shouldSynchronizeTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(stockDailyQuoteCollectionFace.synchronize(tradeDate)).thenReturn(5547);

        assertEquals(5547, stockDailyQuoteSyncService.synchronize(tradeDate));
    }
}
