package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.StockBasicSyncService;
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
 * 股票基础信息定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.basic.sync", name = "enabled", havingValue = "true")
public class StockBasicTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private static final LocalTime SCHEDULED_TIME = LocalTime.of(9, 10);

    private final StockBasicSyncService stockBasicSyncService;

    @Scheduled(cron = "${stock.basic.sync.cron:0 10 9 * * *}", zone = "Asia/Shanghai")
    public void synchronizeOnSchedule() {
        synchronizeIfTradingDay(LocalDate.now(SHANGHAI_ZONE), "SCHEDULED");
    }

    /**
     * 程序在09:10之后启动时，补执行当天尚未完成的股票清单同步。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void synchronizeAfterStartup() {
        synchronizeAfterStartup(LocalDate.now(SHANGHAI_ZONE), LocalTime.now(SHANGHAI_ZONE));
    }

    /**
     * 程序在09:10之后启动时，补执行当天尚未完成的股票清单同步。
     */
    int synchronizeAfterStartup(LocalDate tradeDate, LocalTime currentTime) {
        if (currentTime.isBefore(SCHEDULED_TIME)) {
            return 0;
        }
        try {
            return synchronizeIfTradingDay(tradeDate, "STARTUP_CATCH_UP");
        } catch (RuntimeException exception) {
            log.warn("股票基础信息启动补采失败，应用继续启动，tradeDate={}", tradeDate);
            return 0;
        }
    }

    /**
     * 如果是交易日，则执行股票清单同步。
     */
    int synchronizeIfTradingDay(LocalDate tradeDate, String triggerType) {
        long startTime = System.currentTimeMillis();
        log.info("股票基础信息任务开始，triggerType={}，tradeDate={}", triggerType, tradeDate);
        try {
            int savedCount = stockBasicSyncService.synchronize(tradeDate);
            log.info(
                    "股票基础信息任务完成，triggerType={}，tradeDate={}，savedCount={}，elapsedMs={}",
                    triggerType,
                    tradeDate,
                    savedCount,
                    System.currentTimeMillis() - startTime
            );
            return savedCount;
        } catch (RuntimeException exception) {
            log.error(
                    "股票基础信息任务失败，triggerType={}，tradeDate={}，elapsedMs={}，reason={}",
                    triggerType,
                    tradeDate,
                    System.currentTimeMillis() - startTime,
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }
}
