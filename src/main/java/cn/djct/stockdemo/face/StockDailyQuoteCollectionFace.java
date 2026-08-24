package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import lombok.extern.slf4j.Slf4j;
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

    private static final int MINIMUM_STOCK_COUNT = 3000;
    private static final int STOCK_READ_BATCH_SIZE = 1000;

    private final StockBasicService stockBasicService;
    private final StockDailyQuoteSourceService stockDailyQuoteSourceService;
    private final StockDailyQuoteService stockDailyQuoteService;
    private final ReentrantLock synchronizationLock = new ReentrantLock();

    public StockDailyQuoteCollectionFace(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            StockDailyQuoteService stockDailyQuoteService
    ) {
        this.stockBasicService = stockBasicService;
        this.stockDailyQuoteSourceService = stockDailyQuoteSourceService;
        this.stockDailyQuoteService = stockDailyQuoteService;
    }

    /**
     * 股票日行情同步
     * @param tradeDate 交易日期
     * @return 影响行数
     */
    public int synchronize(LocalDate tradeDate) {
        // 上锁，防止重复执行
        if (!synchronizationLock.tryLock()) {
            log.info("股票日行情同步正在执行，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }
        try {
            // 获取当天股票基础信息数量
            int stockCount = stockBasicService.countSnapshot(tradeDate);
            // 判断当天股票基础信息数量是否达到阈值，数据缺失严重
            if (stockCount < MINIMUM_STOCK_COUNT) {
                throw new IllegalStateException("当天股票基础信息不完整，tradeDate="
                        + tradeDate + "，minimum=" + MINIMUM_STOCK_COUNT + "，actual=" + stockCount);
            }

            // 判断当天股票日行情数量是否与股票基础信息数量一致
            int savedCount = stockDailyQuoteService.countByTradeDate(tradeDate);
            if (savedCount == stockCount) {
                log.info("股票日行情当天已经同步，本次触发跳过，tradeDate={}", tradeDate);
                return 0;
            }
            // 执行全量同步
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

    /**
     * 全量采集
     * @param tradeDate 交易日期
     * @param expectedCount 期望数量
      * @return 采集数量
     */
    private List<StockDailyQuote> collectAll(LocalDate tradeDate, int expectedCount) {
        List<StockDailyQuote> quotes = new ArrayList<>(expectedCount);
        // 上次股票代码
        String lastStockCode = "";
        // 循环读取股票基础信息
        while (true) {
            // 读取股票基础信息批次
            List<StockBasic> stocks = stockBasicService.findSnapshotBatch(
                    tradeDate,
                    lastStockCode,
                    STOCK_READ_BATCH_SIZE
            );
            // 如果没有股票基础信息则退出循环
            if (stocks.isEmpty()) {
                break;
            }
            // 采集股票日行情
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
