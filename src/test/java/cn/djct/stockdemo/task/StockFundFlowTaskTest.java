package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSyncService;
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
class StockFundFlowTaskTest {

    @Mock
    private StockFundFlowSyncService stockFundFlowSyncService;

    @InjectMocks
    private StockFundFlowTask stockFundFlowTask;

    @Test
    void shouldSkipStartupCatchUpBeforeFifteenFive() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 21);

        assertEquals(0, stockFundFlowTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 4)));

        verify(stockFundFlowSyncService, never()).synchronize(tradeDate);
    }

    @Test
    void shouldCatchUpAfterFifteenFive() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 21);
        when(stockFundFlowSyncService.synchronize(tradeDate)).thenReturn(5200);

        assertEquals(5200, stockFundFlowTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 6)));
    }

    @Test
    void shouldKeepApplicationRunningWhenStartupCatchUpFails() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 21);
        doThrow(new IllegalStateException("数据源不可用"))
                .when(stockFundFlowSyncService).synchronize(tradeDate);

        assertEquals(0, stockFundFlowTask.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 6)));
    }
}
