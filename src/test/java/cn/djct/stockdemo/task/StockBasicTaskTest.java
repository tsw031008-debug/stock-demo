package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.StockBasicSyncService;
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
class StockBasicTaskTest {

    @Mock
    private StockBasicSyncService stockBasicSyncService;

    @InjectMocks
    private StockBasicTask stockBasicTask;

    @Test
    void shouldSkipSynchronizationOnNonTradingDay() {
        LocalDate date = LocalDate.of(2026, 8, 22);
        when(stockBasicSyncService.synchronize(date)).thenReturn(0);

        assertEquals(0, stockBasicTask.synchronizeIfTradingDay(date, "TEST"));

        verify(stockBasicSyncService).synchronize(date);
    }

    @Test
    void shouldSynchronizeOnTradingDay() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(stockBasicSyncService.synchronize(date)).thenReturn(5200);

        assertEquals(5200, stockBasicTask.synchronizeIfTradingDay(date, "TEST"));

        verify(stockBasicSyncService).synchronize(date);
    }

    @Test
    void shouldSkipStartupCatchUpBeforeNineTen() {
        LocalDate date = LocalDate.of(2026, 8, 20);

        assertEquals(0, stockBasicTask.synchronizeAfterStartup(date, LocalTime.of(9, 9)));

        verify(stockBasicSyncService, never()).synchronize(date);
    }

    @Test
    void shouldCatchUpAfterNineTenWhenApplicationStartsLate() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(stockBasicSyncService.synchronize(date)).thenReturn(5200);

        assertEquals(5200, stockBasicTask.synchronizeAfterStartup(date, LocalTime.of(9, 11)));

        verify(stockBasicSyncService).synchronize(date);
    }

    @Test
    void shouldKeepApplicationStartupRunningWhenCatchUpFails() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        doThrow(new IllegalStateException("数据源不可用"))
                .when(stockBasicSyncService)
                .synchronize(date);

        assertEquals(0, stockBasicTask.synchronizeAfterStartup(date, LocalTime.of(9, 11)));
    }
}
