package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowService;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSourceService;
import cn.djct.stockdemo.util.StockMarketCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 股票资金流向采集编排。
 */
@Slf4j
@Component
public class StockFundFlowCollectionFace {

    private static final int MINIMUM_STOCK_COUNT = 3000;
    private static final int STOCK_READ_BATCH_SIZE = 1000;

    private final StockBasicService stockBasicService;
    private final StockFundFlowSourceService stockFundFlowSourceService;
    private final StockFundFlowService stockFundFlowService;
    private final ReentrantLock synchronizationLock = new ReentrantLock();

    public StockFundFlowCollectionFace(
            StockBasicService stockBasicService,
            StockFundFlowSourceService stockFundFlowSourceService,
            StockFundFlowService stockFundFlowService
    ) {
        this.stockBasicService = stockBasicService;
        this.stockFundFlowSourceService = stockFundFlowSourceService;
        this.stockFundFlowService = stockFundFlowService;
    }

    // 同步股票资金流向
    public int synchronize(LocalDate tradeDate) {
        // 尝试获取锁
        if (!synchronizationLock.tryLock()) {
            log.info("股票资金流向同步正在执行，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }
        try {
            // 加载沪深A股清单
            List<StockBasic> stocks = loadShanghaiAndShenzhenStocks(tradeDate);
            if (stocks.size() < MINIMUM_STOCK_COUNT) {
                throw new IllegalStateException("当天沪深A股清单不完整，tradeDate="
                        + tradeDate + "，minimum=" + MINIMUM_STOCK_COUNT
                        + "，actual=" + stocks.size());
            }

            // 如果当天已经同步过，则跳过
            int savedCount = stockFundFlowService.countByTradeDate(tradeDate);
            if (savedCount == stocks.size()) {
                log.info("股票资金流向当天已经同步，本次触发跳过，tradeDate={}", tradeDate);
                return 0;
            }
            // 如果当天已经同步过部分数据，则执行全量幂等补采
            if (savedCount > 0) {
                log.warn("当天股票资金流向不完整，执行全量幂等补采，tradeDate={}，expected={}，actual={}",
                        tradeDate, stocks.size(), savedCount);
            }
            // 从东方财富获取资金流向数据
            // 保存资金流向数据

            List<StockFundFlow> fundFlows = stockFundFlowSourceService.fetchAll(tradeDate, stocks);
            if (fundFlows.size() != stocks.size()) {
                throw new IllegalStateException("东方财富资金流向总数量不一致，expected="
                        + stocks.size() + "，actual=" + fundFlows.size());
            }
            int insertedCount = stockFundFlowService.saveSnapshot(fundFlows);
            log.info("股票资金流向同步完成，tradeDate={}，savedCount={}", tradeDate, insertedCount);
            return insertedCount;
        } finally {
            synchronizationLock.unlock();
        }
    }

    private List<StockBasic> loadShanghaiAndShenzhenStocks(LocalDate tradeDate) {
        List<StockBasic> stocks = new ArrayList<>();
        String lastStockCode = "";
        while (true) {
            List<StockBasic> batch = stockBasicService.findSnapshotBatch(
                    tradeDate,
                    lastStockCode,
                    STOCK_READ_BATCH_SIZE
            );
            if (batch.isEmpty()) {
                break;
            }
            batch.stream()
                    .filter(stock -> StockMarketCodeUtil.isShanghaiOrShenzhenAStock(
                            stock.getStockCode()
                    ))
                    .forEach(stocks::add);
            lastStockCode = batch.get(batch.size() - 1).getStockCode();
        }
        return stocks;
    }
}
