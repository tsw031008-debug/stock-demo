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
        }
    }
}
