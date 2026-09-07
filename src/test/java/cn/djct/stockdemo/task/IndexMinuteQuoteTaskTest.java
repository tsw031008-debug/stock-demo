package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSyncService;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceSignalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexMinuteQuoteTaskTest {

    @Mock
    private IndexMinuteQuoteSyncService indexMinuteQuoteSyncService;

    @Mock
    private IndexDivergenceSignalService indexDivergenceSignalService;

    @InjectMocks
    private IndexMinuteQuoteTask indexMinuteQuoteTask;

    @Test
    void shouldDelegateSynchronization() {
        LocalDateTime triggerTime = LocalDateTime.of(2026, 8, 27, 9, 31, 10);
        when(indexMinuteQuoteSyncService.synchronize(triggerTime)).thenReturn(1);
        when(indexDivergenceSignalService.calculateAndSave(triggerTime.withSecond(0)))
                .thenReturn(0);

        assertEquals(1, indexMinuteQuoteTask.synchronize(triggerTime, "TEST"));

        verify(indexMinuteQuoteSyncService).synchronize(triggerTime);
        verify(indexDivergenceSignalService).calculateAndSave(triggerTime.withSecond(0));
    }

    @Test
    void shouldPropagateSynchronizationFailure() {
        LocalDateTime triggerTime = LocalDateTime.of(2026, 8, 27, 9, 31, 10);
        doThrow(new IllegalStateException("数据源不可用"))
                .when(indexMinuteQuoteSyncService).synchronize(triggerTime);

        assertThrows(
                IllegalStateException.class,
                () -> indexMinuteQuoteTask.synchronize(triggerTime, "TEST")
        );
    }

    @Test
    void shouldDelegateMinuteRecovery() {
        LocalDateTime checkTime = LocalDateTime.of(2026, 8, 27, 15, 2, 30);
        when(indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime)).thenReturn(2);

        assertEquals(2, indexMinuteQuoteTask.recover(checkTime, "TEST"));

        verify(indexMinuteQuoteSyncService).recoverMissingMinutes(checkTime);
    }

    @Test
    void shouldKeepApplicationStartupWhenRecoveryFails() {
        LocalDateTime checkTime = LocalDateTime.of(2026, 8, 27, 10, 0);
        doThrow(new IllegalStateException("分时接口不可用"))
                .when(indexMinuteQuoteSyncService).recoverMissingMinutes(checkTime);

        assertEquals(0, indexMinuteQuoteTask.recoverAfterStartup(checkTime));
    }
}
