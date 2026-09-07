package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexEtfDailyQuoteTaskTest {

    @Mock
    private IndexEtfDailyQuoteSyncService syncService;
    @InjectMocks
    private IndexEtfDailyQuoteTask task;

    @Test
    void shouldDelegateSynchronization() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 4);
        when(syncService.synchronize(tradeDate)).thenReturn(4);

        assertEquals(4, task.synchronize(tradeDate, "TEST"));
    }

    @Test
    void shouldSkipStartupCatchUpBeforeScheduledTime() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 4);

        assertEquals(0, task.synchronizeAfterStartup(tradeDate, LocalTime.of(15, 6)));

        verify(syncService, never()).synchronize(tradeDate);
    }
}
