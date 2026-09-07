package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSyncService;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 上证指数分钟行情定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "stock.index-divergence.minute-quote.sync",
        name = "enabled",
        havingValue = "true"
)
public class IndexMinuteQuoteTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final IndexMinuteQuoteSyncService indexMinuteQuoteSyncService;
    private final IndexDivergenceSignalService indexDivergenceSignalService;

    /**
     * 09:00至15:59每分钟触发，Service只处理240个有效分钟。
     */
    @Scheduled(
            cron = "${stock.index-divergence.minute-quote.sync.cron:10 * 9-15 * * *}",
            zone = "Asia/Shanghai"
    )
    public void synchronizeOnSchedule() {
        synchronize(LocalDateTime.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    /**
     * 收盘后检查全天240个分钟时点，只有存在缺口时才请求腾讯分时接口。
     */
    @Scheduled(
            cron = "${stock.index-divergence.minute-quote.sync.recovery-cron:30 2 15 * * *}",
            zone = "Asia/Shanghai"
    )
    public void recoverAfterClose() {
        recover(LocalDateTime.now(SHANGHAI_ZONE), "SCHEDULED_COMPLETENESS_CHECK");
    }

    /**
     * 应用启动时检查一次，恢复盘中意外停机造成的已有分钟缺口。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(30)
    public void recoverAfterStartup() {
        recoverAfterStartup(LocalDateTime.now(SHANGHAI_ZONE));
    }

    int recoverAfterStartup(LocalDateTime checkTime) {
        try {
            return recover(checkTime, "STARTUP_CATCH_UP");
        } catch (RuntimeException exception) {
            log.warn("上证指数分钟行情启动补采失败，应用继续启动，reason={}",
                    exception.getMessage());
            return 0;
        }
    }

    /**
     * 执行分钟行情同步并记录状态。
     */
    int synchronize(LocalDateTime triggerTime, String triggerType) {
        long startTime = System.currentTimeMillis();
        try {
            int savedCount = indexMinuteQuoteSyncService.synchronize(triggerTime);
            if (savedCount > 0) {
                // 获取 quoteMinute
                LocalDateTime quoteMinute = triggerTime.truncatedTo(ChronoUnit.MINUTES);
                // 计算并保存 divergence signal
                int signalCount = indexDivergenceSignalService.calculateAndSave(quoteMinute);
                log.info("上证指数分钟行情任务完成，triggerType={}，quoteMinute={}，savedCount={}，signalCount={}，elapsedMs={}",
                        triggerType, quoteMinute, savedCount, signalCount,
                        System.currentTimeMillis() - startTime);
            }
            return savedCount;
        } catch (RuntimeException exception) {
            log.error("上证指数分钟行情任务失败，triggerType={}，triggerTime={}，elapsedMs={}，reason={}",
                    triggerType, triggerTime, System.currentTimeMillis() - startTime,
                    exception.getMessage(), exception);
            throw exception;
        }
    }

    /**
     * 执行分钟完整性检查和缺口补录，并记录处理结果。
     */
    int recover(LocalDateTime checkTime, String triggerType) {
        long startTime = System.currentTimeMillis();
        try {
            int savedCount = indexMinuteQuoteSyncService.recoverMissingMinutes(checkTime);
            log.info("上证指数分钟完整性检查完成，triggerType={}，checkTime={}，savedCount={}，elapsedMs={}",
                    triggerType, checkTime, savedCount, System.currentTimeMillis() - startTime);
            return savedCount;
        } catch (RuntimeException exception) {
            log.error("上证指数分钟完整性检查失败，triggerType={}，checkTime={}，elapsedMs={}，reason={}",
                    triggerType, checkTime, System.currentTimeMillis() - startTime,
                    exception.getMessage(), exception);
            throw exception;
        }
    }
}
