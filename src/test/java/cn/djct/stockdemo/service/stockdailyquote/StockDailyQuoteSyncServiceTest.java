package cn.djct.stockdemo.service.stockdailyquote;

import cn.djct.stockdemo.face.StockDailyQuoteCollectionFace;
import cn.djct.stockdemo.service.stockdailyquote.impl.StockDailyQuoteSyncServiceImpl;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDailyQuoteSyncServiceTest {

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace;

    private StockDailyQuoteSyncServiceImpl stockDailyQuoteSyncService;

    @BeforeEach
    void setUp() {
        stockDailyQuoteSyncService = new StockDailyQuoteSyncServiceImpl(tradeCalendarService, stockDailyQuoteCollectionFace,
                Clock.fixed(Instant.parse("2026-08-20T08:00:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void shouldRejectIntradayManualSynchronization() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        stockDailyQuoteSyncService = new StockDailyQuoteSyncServiceImpl(tradeCalendarService, stockDailyQuoteCollectionFace,
                Clock.fixed(Instant.parse("2026-08-20T02:00:00Z"), ZoneId.of("Asia/Shanghai")));
        assertThrows(IllegalStateException.class, () -> stockDailyQuoteSyncService.synchronize(tradeDate));
        verify(stockDailyQuoteCollectionFace, never()).synchronize(tradeDate);
    }


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
