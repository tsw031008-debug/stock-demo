package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.stockalert.PlatformBreakoutService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 交易日15:15触发平台突破，计算和持久化由Service处理。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.alert.platform-breakout", name = "enabled", havingValue = "true")
public class PlatformBreakoutTask {
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private final PlatformBreakoutService platformBreakoutService;
    private final TradeCalendarService tradeCalendarService;

    /** 单机串行触发，Service事务结束后释放锁；非交易日跳过。 */
    @Scheduled(cron = "0 15 15 * * *", zone = "Asia/Shanghai")
    public synchronized void selectOnSchedule() {
        select(LocalDate.now(SHANGHAI_ZONE));
    }

    void select(LocalDate date) {
        long started = System.currentTimeMillis();
        log.info("平台突破任务开始，tradeDate={}，startTime={}", date, LocalDateTime.now(SHANGHAI_ZONE));
        try {
            if (!tradeCalendarService.isTradingDay(date)) {
                log.info("非交易日，平台突破任务结束，tradeDate={}，status=SKIPPED，processedCount=0，endTime={}，elapsedMs={}",
                        date, LocalDateTime.now(SHANGHAI_ZONE), System.currentTimeMillis() - started);
                return;
            }
            int count = platformBreakoutService.selectStocks(date);
            log.info("平台突破任务结束，tradeDate={}，status=COMPLETED，selectedCount={}，endTime={}，elapsedMs={}",
                    date, count, LocalDateTime.now(SHANGHAI_ZONE), System.currentTimeMillis() - started);
        } catch (RuntimeException exception) {
            log.error("平台突破任务结束，tradeDate={}，status=FAILED，endTime={}，elapsedMs={}，reason={}",
                    date, LocalDateTime.now(SHANGHAI_ZONE), System.currentTimeMillis() - started, exception.getMessage());
        }
    }
}
