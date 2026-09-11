package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.stockalert.PlatformBreakoutService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformBreakoutTaskTest {
    private final PlatformBreakoutService service = mock(PlatformBreakoutService.class);
    private final TradeCalendarService calendar = mock(TradeCalendarService.class);
    private final PlatformBreakoutTask task = new PlatformBreakoutTask(service, calendar);

    @Test
    void shouldSkipWeekendAndHoliday() {
        task.select(LocalDate.of(2026, 9, 12));
        task.select(LocalDate.of(2026, 1, 1));
        verifyNoInteractions(service);
    }

    @Test
    void shouldRunAndContainFailureOnTradingDay() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(calendar.isTradingDay(date)).thenReturn(true);
        when(service.selectStocks(date)).thenReturn(3).thenThrow(new IllegalStateException("日线缺失"));
        task.select(date);
        assertDoesNotThrow(() -> task.select(date));
        verify(service, times(2)).selectStocks(date);
    }

    @Test
    void shouldHoldTaskLockUntilServiceReturnsAndReleaseAfterFailure() throws Exception {
        when(calendar.isTradingDay(any())).thenReturn(true);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(service.selectStocks(any())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待超时");
            }
            throw new IllegalStateException("模拟事务回滚");
        }).thenReturn(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(task::selectOnSchedule);
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var second = executor.submit(task::selectOnSchedule);
            assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> second.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            verify(service, times(1)).selectStocks(any());
            release.countDown();
            first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            second.get(5, java.util.concurrent.TimeUnit.SECONDS);
            verify(service, times(2)).selectStocks(any());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldScheduleAt1515ShanghaiTime() throws Exception {
        Scheduled scheduled = PlatformBreakoutTask.class.getMethod("selectOnSchedule").getAnnotation(Scheduled.class);
        assertEquals("0 15 15 * * *", scheduled.cron());
        assertEquals("Asia/Shanghai", scheduled.zone());
    }
}
