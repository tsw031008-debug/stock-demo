package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSyncService;
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
 * 四只固定指数ETF日行情同步任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.index-style.etf-daily-quote.sync", name = "enabled",
        havingValue = "true")
public class IndexEtfDailyQuoteTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime SCHEDULED_TIME = LocalTime.of(15, 7);

    private final IndexEtfDailyQuoteSyncService indexEtfDailyQuoteSyncService;

    @Scheduled(cron = "${stock.index-style.etf-daily-quote.sync.cron:0 7 15 * * *}",
            zone = "Asia/Shanghai")
    public void synchronizeOnSchedule() {
        synchronize(LocalDate.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(26)
    public void synchronizeAfterStartup() {
        synchronizeAfterStartup(LocalDate.now(SHANGHAI_ZONE), LocalTime.now(SHANGHAI_ZONE));
    }

    /**
     * 应用在15:07之后启动时补执行当天ETF日行情同步。
     */
    int synchronizeAfterStartup(LocalDate tradeDate, LocalTime currentTime) {
        if (currentTime.isBefore(SCHEDULED_TIME)) {
            return 0;
        }
        try {
            return synchronize(tradeDate, "STARTUP_CATCH_UP");
        } catch (RuntimeException exception) {
            log.warn("指数ETF日行情启动补采失败，应用继续启动，tradeDate={}", tradeDate);
            return 0;
        }
    }

    /**
     * 执行ETF日行情同步并记录处理数量和耗时。
     */
    int synchronize(LocalDate tradeDate, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("指数ETF日行情任务开始，triggerType={}，tradeDate={}", triggerType, tradeDate);
        try {
            int savedCount = indexEtfDailyQuoteSyncService.synchronize(tradeDate);
            log.info("指数ETF日行情任务完成，triggerType={}，tradeDate={}，savedCount={}，elapsedMs={}",
                    triggerType, tradeDate, savedCount, System.currentTimeMillis() - startTime);
            return savedCount;
        } catch (RuntimeException exception) {
            log.error("指数ETF日行情任务失败，triggerType={}，tradeDate={}，elapsedMs={}，reason={}",
                    triggerType, tradeDate, System.currentTimeMillis() - startTime,
                    exception.getMessage(), exception);
            throw exception;
        }
    }
}
