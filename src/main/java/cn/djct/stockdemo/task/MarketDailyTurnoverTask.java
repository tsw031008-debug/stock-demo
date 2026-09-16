package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.marketlevel.MarketDailyTurnoverService;
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
import java.util.List;

/**
 * 每日市场成交额汇总任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.market-level.daily-turnover.sync", name = "enabled",
        havingValue = "true")
public class MarketDailyTurnoverTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime SCHEDULED_TIME = LocalTime.of(15, 4);

    private final MarketDailyTurnoverService marketDailyTurnoverService;

    @Scheduled(cron = "${stock.market-level.daily-turnover.sync.cron:0 4 15 * * *}",
            zone = "Asia/Shanghai")
    public void synchronizeOnSchedule() {
        synchronize(LocalDate.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(22)
    public void synchronizeAfterStartup() {
        reportRecentGaps(LocalDate.now(SHANGHAI_ZONE).minusDays(1));
        LocalTime currentTime = LocalTime.now(SHANGHAI_ZONE);
        if (!currentTime.isBefore(SCHEDULED_TIME)) {
            try {
                synchronize(LocalDate.now(SHANGHAI_ZONE), "STARTUP_CATCH_UP");
            } catch (RuntimeException exception) {
                log.warn("每日市场成交额启动补算失败，应用继续启动");
            }
        }
    }

    int synchronize(LocalDate tradeDate, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("每日市场成交额任务开始，triggerType={}，tradeDate={}", triggerType, tradeDate);
        try {
            int savedCount = marketDailyTurnoverService.synchronize(tradeDate);
            log.info("每日市场成交额任务完成，triggerType={}，tradeDate={}，savedCount={}，elapsedMs={}",
                    triggerType, tradeDate, savedCount, System.currentTimeMillis() - startTime);
            return savedCount;
        } catch (RuntimeException exception) {
            log.error("每日市场成交额任务失败，triggerType={}，tradeDate={}，elapsedMs={}，reason={}",
                    triggerType, tradeDate, System.currentTimeMillis() - startTime,
                    exception.getMessage(), exception);
            throw exception;
        } finally {
            reportRecentGaps(tradeDate);
        }
    }

    private void reportRecentGaps(LocalDate endDate) {
        try {
            List<LocalDate> missingDates = marketDailyTurnoverService.findMissingTradeDates(
                    endDate.minusDays(30), endDate);
            if (!missingDates.isEmpty()) {
                log.warn("最近31个自然日存在市场成交额汇总缺口，请核验原始日线后按日补算，missingDates={}", missingDates);
            }
        } catch (RuntimeException exception) {
            log.warn("市场成交额历史缺口检查失败，endDate={}，reason={}", endDate, exception.getMessage());
        }
    }
}
