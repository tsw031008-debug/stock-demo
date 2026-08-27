package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.plate.StockPlateDailyQuoteService;
import cn.djct.stockdemo.service.plate.StockPlateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 股票板块定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.plate.sync", name = "enabled", havingValue = "true")
public class StockPlateTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime MEMBER_SYNC_TIME = LocalTime.of(9, 15);
    private static final LocalTime DAILY_QUOTE_SYNC_TIME = LocalTime.of(15, 3);

    private final StockPlateSyncService stockPlateSyncService;
    private final StockPlateDailyQuoteService stockPlateDailyQuoteService;

    /**
     * 每个交易日09:15同步板块和成分股快照。
     */
    @Scheduled(cron = "${stock.plate.sync.member-cron:0 15 9 * * *}", zone = "Asia/Shanghai")
    public void synchronizeMembersOnSchedule() {
        synchronizeMembers(LocalDate.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    /**
     * 股票日行情完成后计算并保存当天板块日线。
     */
    @Scheduled(cron = "${stock.plate.sync.daily-quote-cron:0 3 15 * * *}", zone = "Asia/Shanghai")
    public void synchronizeDailyQuotesOnSchedule() {
        synchronizeDailyQuotes(LocalDate.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    /**
     * 应用启动后按依赖顺序补执行当天板块任务。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(25)
    public void synchronizeAfterStartup() {
        synchronizeAfterStartup(LocalDate.now(SHANGHAI_ZONE), LocalTime.now(SHANGHAI_ZONE));
    }

    /**
     * 根据启动时间补执行板块和板块日线任务。
     */
    int synchronizeAfterStartup(LocalDate tradeDate, LocalTime currentTime) {
        int processedCount = 0;
        if (!currentTime.isBefore(MEMBER_SYNC_TIME)) {
            processedCount += runSafely(() -> synchronizeMembers(tradeDate, "STARTUP_CATCH_UP"));
        }
        if (!currentTime.isBefore(DAILY_QUOTE_SYNC_TIME)) {
            processedCount += runSafely(() -> synchronizeDailyQuotes(tradeDate, "STARTUP_CATCH_UP"));
        }
        return processedCount;
    }

    /**
     * 执行板块成分股同步并记录耗时。
     */
    int synchronizeMembers(LocalDate tradeDate, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("板块同步任务开始，triggerType={}，tradeDate={}", triggerType, tradeDate);
        int savedCount = stockPlateSyncService.synchronize(tradeDate);
        log.info("板块同步任务完成，triggerType={}，tradeDate={}，savedCount={}，elapsedMs={}",
                triggerType, tradeDate, savedCount, System.currentTimeMillis() - startTime);
        return savedCount;
    }

    /**
     * 执行板块日线同步并记录耗时。
     */
    int synchronizeDailyQuotes(LocalDate tradeDate, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("板块日线任务开始，triggerType={}，tradeDate={}", triggerType, tradeDate);
        int savedCount = stockPlateDailyQuoteService.synchronize(tradeDate);
        log.info("板块日线任务完成，triggerType={}，tradeDate={}，savedCount={}，elapsedMs={}",
                triggerType, tradeDate, savedCount, System.currentTimeMillis() - startTime);
        return savedCount;
    }

    /**
     * 启动补采失败时记录错误并允许应用继续启动。
     */
    private int runSafely(TaskOperation operation) {
        try {
            return operation.execute();
        } catch (RuntimeException exception) {
            log.warn("板块启动补采失败，应用继续启动，reason={}", exception.getMessage());
            return 0;
        }
    }

    /**
     * 启动补采操作。
     */
    @FunctionalInterface
    private interface TaskOperation {
        int execute();
    }
}
