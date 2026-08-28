package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSyncService;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
     * 执行分钟行情同步并记录状态。
     */
    int synchronize(LocalDateTime triggerTime, String triggerType) {
        long startTime = System.currentTimeMillis();
        try {
            int savedCount = indexMinuteQuoteSyncService.synchronize(triggerTime);
            if (savedCount > 0) {
                // 获取 quoteMinute
                LocalDateTime quoteMinute = triggerTime.withSecond(0).withNano(0);
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
}
