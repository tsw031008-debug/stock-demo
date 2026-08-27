package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.plate.StockPlateDailyQuoteService;
import cn.djct.stockdemo.service.plate.StockPlateSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockPlateTaskTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 26);

    @Mock
    private StockPlateSyncService stockPlateSyncService;
    @Mock
    private StockPlateDailyQuoteService stockPlateDailyQuoteService;

    private AutoCloseable mocks;
    private StockPlateTask task;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        task = new StockPlateTask(stockPlateSyncService, stockPlateDailyQuoteService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldSkipBothTasksBeforeMemberSyncTime() {
        assertEquals(0, task.synchronizeAfterStartup(TRADE_DATE, LocalTime.of(9, 14)));
        verifyNoInteractions(stockPlateSyncService, stockPlateDailyQuoteService);
    }

    @Test
    void shouldRunMembersAndDailyQuoteAfterClose() {
        when(stockPlateSyncService.synchronize(TRADE_DATE)).thenReturn(100);
        when(stockPlateDailyQuoteService.synchronize(TRADE_DATE)).thenReturn(80);

        assertEquals(180, task.synchronizeAfterStartup(TRADE_DATE, LocalTime.of(15, 4)));
    }
}
