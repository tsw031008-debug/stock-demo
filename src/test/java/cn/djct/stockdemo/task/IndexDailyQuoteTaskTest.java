package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSyncService;
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
class IndexDailyQuoteTaskTest {

    @Mock
    private IndexDailyQuoteSyncService indexDailyQuoteSyncService;
    @InjectMocks
    private IndexDailyQuoteTask indexDailyQuoteTask;

    @Test
    void shouldDelegateScheduledSynchronization() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 3);
        when(indexDailyQuoteSyncService.synchronize(tradeDate)).thenReturn(4);

        assertEquals(4, indexDailyQuoteTask.synchronize(tradeDate, "TEST"));
    }

    @Test
    void shouldSkipStartupCatchUpBeforeScheduledTime() {
        LocalDate tradeDate = LocalDate.of(2026, 9, 3);

        assertEquals(0, indexDailyQuoteTask.synchronizeAfterStartup(
                tradeDate,
                LocalTime.of(15, 5)
        ));
        verify(indexDailyQuoteSyncService, never()).synchronize(tradeDate);
    }
}
