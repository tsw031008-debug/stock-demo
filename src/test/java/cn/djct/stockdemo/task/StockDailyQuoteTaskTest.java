package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDailyQuoteTaskTest {

    @Mock
    private StockDailyQuoteSyncService stockDailyQuoteSyncService;

    @InjectMocks
    private StockDailyQuoteTask stockDailyQuoteTask;

    @Test
    void shouldSkipStartupCatchUpBeforeFifteenTwo() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);

        assertEquals(0, stockDailyQuoteTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 1)));

        verify(stockDailyQuoteSyncService, never()).synchronize(tradeDate);
    }

    @Test
    void shouldCatchUpAfterFifteenTwo() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        when(stockDailyQuoteSyncService.synchronize(tradeDate)).thenReturn(5547);

        assertEquals(5547, stockDailyQuoteTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 3)));
    }

    @Test
    void shouldKeepApplicationRunningWhenStartupCatchUpFails() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        doThrow(new IllegalStateException("数据源不可用"))
                .when(stockDailyQuoteSyncService).synchronize(tradeDate);

        assertEquals(0, stockDailyQuoteTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 3)));
    }
}
