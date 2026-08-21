package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockDailyQuoteService;
import cn.djct.stockdemo.service.StockDailyQuoteSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 股票日行情采集编排。
 */
@Slf4j
@Component
public class StockDailyQuoteCollectionFace {

    private static final int STOCK_READ_BATCH_SIZE = 1000;

    private final StockBasicService stockBasicService;
    private final StockDailyQuoteSourceService stockDailyQuoteSourceService;
    private final StockDailyQuoteService stockDailyQuoteService;
    private final int minimumStockCount;
    private final ReentrantLock synchronizationLock = new ReentrantLock();

    public StockDailyQuoteCollectionFace(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            StockDailyQuoteService stockDailyQuoteService,
            @Value("${stock.basic.sync.minimum-stock-count:3000}") int minimumStockCount
    ) {
        this.stockBasicService = stockBasicService;
        this.stockDailyQuoteSourceService = stockDailyQuoteSourceService;
        this.stockDailyQuoteService = stockDailyQuoteService;
        this.minimumStockCount = minimumStockCount;
    }

    public int synchronize(LocalDate tradeDate) {
        if (!synchronizationLock.tryLock()) {
            log.info("股票日行情同步正在执行，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }
        try {
            int stockCount = stockBasicService.countSnapshot(tradeDate);
            if (stockCount < minimumStockCount) {
                throw new IllegalStateException("当天股票基础信息不完整，tradeDate="
                        + tradeDate + "，minimum=" + minimumStockCount + "，actual=" + stockCount);
            }

            int savedCount = stockDailyQuoteService.countByTradeDate(tradeDate);
            if (savedCount == stockCount) {
                log.info("股票日行情当天已经同步，本次触发跳过，tradeDate={}", tradeDate);
                return 0;
            }
            if (savedCount > 0) {
                log.warn("当天股票日行情不完整，执行全量幂等补采，tradeDate={}，expected={}，actual={}",
                        tradeDate, stockCount, savedCount);
            }

            List<StockDailyQuote> quotes = collectAll(tradeDate, stockCount);
            int insertedCount = stockDailyQuoteService.saveSnapshot(quotes);
            log.info("股票日行情同步完成，tradeDate={}，savedCount={}", tradeDate, insertedCount);
            return insertedCount;
        } finally {
            synchronizationLock.unlock();
        }
    }

    private List<StockDailyQuote> collectAll(LocalDate tradeDate, int expectedCount) {
        List<StockDailyQuote> quotes = new ArrayList<>(expectedCount);
        String lastStockCode = "";
        while (true) {
            List<StockBasic> stocks = stockBasicService.findSnapshotBatch(
                    tradeDate,
                    lastStockCode,
                    STOCK_READ_BATCH_SIZE
            );
            if (stocks.isEmpty()) {
                break;
            }
            List<StockDailyQuote> batchQuotes = stockDailyQuoteSourceService.fetchAll(tradeDate, stocks);
            if (batchQuotes.size() != stocks.size()) {
                throw new IllegalStateException("腾讯行情批次数量不一致，expected="
                        + stocks.size() + "，actual=" + batchQuotes.size());
            }
            quotes.addAll(batchQuotes);
            lastStockCode = stocks.get(stocks.size() - 1).getStockCode();
        }

        if (quotes.size() != expectedCount) {
            throw new IllegalStateException("腾讯行情总数量不一致，expected="
                    + expectedCount + "，actual=" + quotes.size());
        }
        return quotes;
    }
}
