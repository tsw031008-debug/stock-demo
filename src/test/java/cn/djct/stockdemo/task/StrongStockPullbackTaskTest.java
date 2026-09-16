package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.stockalert.StrongStockPullbackService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StrongStockPullbackTaskTest {
    private final StrongStockPullbackService strongStockPullbackService = mock(StrongStockPullbackService.class);
    private final TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
    private final StrongStockPullbackTask task = new StrongStockPullbackTask(strongStockPullbackService, tradeCalendarService);

    @Test
    void shouldCatchUpOnlyAfter1515OnUnfinishedTradingDay() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        task.selectAfterStartup(date, java.time.LocalTime.of(15, 14, 59));
        verifyNoInteractions(strongStockPullbackService, tradeCalendarService);
        task.selectAfterStartup(date, java.time.LocalTime.of(16, 0));
        verifyNoInteractions(strongStockPullbackService);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(strongStockPullbackService.isCompleted(date)).thenReturn(true);
        task.selectAfterStartup(date, java.time.LocalTime.of(16, 0));
        verify(strongStockPullbackService, never()).selectStocks(any());
        when(strongStockPullbackService.isCompleted(date)).thenReturn(false);
        task.selectAfterStartup(date, java.time.LocalTime.of(15, 15));
        verify(strongStockPullbackService).selectStocks(date);
    }

    @Test
    void shouldContainStartupFailuresAndRunAfterDailyQuoteStartup() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(strongStockPullbackService.isCompleted(date)).thenThrow(new IllegalStateException("数据库异常"));
        assertDoesNotThrow(() -> task.selectAfterStartup(date, java.time.LocalTime.of(16, 0)));
        var method = StrongStockPullbackTask.class.getMethod("selectAfterStartup");
        assertTrue(method.isAnnotationPresent(org.springframework.context.event.EventListener.class));
        int dailyOrder = StockDailyQuoteTask.class.getMethod("synchronizeAfterStartup")
                .getAnnotation(org.springframework.core.annotation.Order.class).value();
        assertTrue(method.getAnnotation(org.springframework.core.annotation.Order.class).value() > dailyOrder);
        assertTrue(java.lang.reflect.Modifier.isSynchronized(StrongStockPullbackTask.class
                .getDeclaredMethod("selectAfterStartup", LocalDate.class, java.time.LocalTime.class).getModifiers()));
    }

    @Test
    void shouldSkipWeekendAndHoliday() {
        task.select(LocalDate.of(2026, 9, 12));
        task.select(LocalDate.of(2026, 1, 1));
        verifyNoInteractions(strongStockPullbackService);
    }

    @Test
    void shouldRunAndContainFailureOnTradingDay() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(strongStockPullbackService.selectStocks(date)).thenReturn(3).thenThrow(new IllegalStateException("日线缺失"));
        task.select(date);
        assertDoesNotThrow(() -> task.select(date));
        verify(strongStockPullbackService, times(2)).selectStocks(date);
    }

    @Test
    void shouldHoldTaskLockUntilServiceReturnsAndReleaseAfterFailure() throws Exception {
        when(tradeCalendarService.isTradingDay(any())).thenReturn(true);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(strongStockPullbackService.selectStocks(any())).thenAnswer(invocation -> {
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
            verify(strongStockPullbackService, times(1)).selectStocks(any());
            release.countDown();
            first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            second.get(5, java.util.concurrent.TimeUnit.SECONDS);
            verify(strongStockPullbackService, times(2)).selectStocks(any());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldScheduleAt1515ShanghaiTime() throws Exception {
        Scheduled scheduled = StrongStockPullbackTask.class.getMethod("selectOnSchedule").getAnnotation(Scheduled.class);
        assertEquals("0 15 15 * * *", scheduled.cron());
        assertEquals("Asia/Shanghai", scheduled.zone());
    }
}
