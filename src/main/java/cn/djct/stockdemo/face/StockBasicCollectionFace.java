package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockBasicSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 股票基础信息采集编排。
 */
@Slf4j
@Component
public class StockBasicCollectionFace {

    private static final int MINIMUM_RETAIN_PERCENT = 95;

    private final StockBasicSourceService stockBasicSourceService;

    private final StockBasicService stockBasicService;

    private final int minimumStockCount;

    private final ReentrantLock synchronizationLock = new ReentrantLock();

    public StockBasicCollectionFace(
            StockBasicSourceService stockBasicSourceService,
            StockBasicService stockBasicService,
            @Value("${stock.basic.sync.minimum-stock-count:3000}") int minimumStockCount
    ) {
        this.stockBasicSourceService = stockBasicSourceService;
        this.stockBasicService = stockBasicService;
        this.minimumStockCount = minimumStockCount;
    }

    public int synchronize(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        if (!synchronizationLock.tryLock()) {
            log.info("股票基础信息同步正在执行，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }

        try {
            if (stockBasicService.hasSynchronized(tradeDate)) {
                log.info("股票基础信息当天已经同步，本次触发跳过，tradeDate={}", tradeDate);
                return 0;
            }

            List<StockBasicDto> stocks = stockBasicSourceService.fetchAll();
            validateSnapshot(stocks, stockBasicService.countLatestSnapshot());
            int savedCount = stockBasicService.saveSnapshot(tradeDate, stocks);
            log.info("股票基础信息同步完成，tradeDate={}，savedCount={}", tradeDate, savedCount);
            return savedCount;
        } finally {
            synchronizationLock.unlock();
        }
    }

    private void validateSnapshot(List<StockBasicDto> stocks, int latestSnapshotCount) {
        if (stocks.size() < minimumStockCount) {
            throw new IllegalStateException(
                    "股票清单数量过少，minimum=" + minimumStockCount + "，actual=" + stocks.size()
            );
        }

        Set<String> stockCodes = new HashSet<>(stocks.size());
        for (StockBasicDto stock : stocks) {
            if (!stockCodes.add(stock.getStockCode())) {
                throw new IllegalStateException("股票清单存在重复代码：" + stock.getStockCode());
            }
        }

        if (latestSnapshotCount > 0
                && stocks.size() * 100L < latestSnapshotCount * (long) MINIMUM_RETAIN_PERCENT) {
            throw new IllegalStateException(
                    "股票清单相比上次异常减少，previous=" + latestSnapshotCount + "，actual=" + stocks.size()
            );
        }
    }
}
