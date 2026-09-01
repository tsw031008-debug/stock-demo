package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.service.stockdailyquote.StockQuoteSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 全市场实时行情快照服务实现。
 */
@Service
public class StockQuoteSnapshotServiceImpl implements StockQuoteSnapshotService {

    private static final int STOCK_BATCH_SIZE = 1000;
    private static final int MINIMUM_STOCK_COUNT = 3000;
    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(60);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockBasicService stockBasicService;
    private final StockDailyQuoteSourceService stockDailyQuoteSourceService;
    private final Clock clock;
    private final Object snapshotLock = new Object();

    // 当前交易日全市场行情快照
    private volatile QuoteSnapshot quoteSnapshot;

    /**
     * 创建使用系统时钟的全市场行情快照服务。
     *
     * @param stockBasicService            股票基础信息服务
     * @param stockDailyQuoteSourceService 股票实时行情数据源
     */
    @Autowired
    public StockQuoteSnapshotServiceImpl(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService
    ) {
        this(
                stockBasicService,
                stockDailyQuoteSourceService,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的快照服务，供缓存边界测试使用。
     */
    StockQuoteSnapshotServiceImpl(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            Clock clock
    ) {
        this.stockBasicService = stockBasicService;
        this.stockDailyQuoteSourceService = stockDailyQuoteSourceService;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 获取指定交易日的60秒全市场实时行情快照。
     *
     * @param tradeDate 交易日
     * @return 全市场实时行情
     */
    @Override
    public List<StockDailyQuote> getSnapshot(LocalDate tradeDate) {
        // 无锁读取尚未过期的当天快照
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Instant now = Instant.now(clock);
        QuoteSnapshot currentSnapshot = quoteSnapshot;
        if (isSnapshotAvailable(currentSnapshot, tradeDate, now)) {
            return currentSnapshot.quotes();
        }
        // 快照失效时加锁，保证同一时刻只有一个线程请求全市场行情
        synchronized (snapshotLock) {
            // 获取锁后再次检查，避免等待期间其他线程已经完成刷新
            currentSnapshot = quoteSnapshot;
            now = Instant.now(clock);
            if (isSnapshotAvailable(currentSnapshot, tradeDate, now)) {
                return currentSnapshot.quotes();
            }
            // 刷新失败时直接抛出异常，不使用过期行情
            List<StockDailyQuote> quotes = fetchCurrentQuotes(tradeDate);
            quoteSnapshot = new QuoteSnapshot(
                    tradeDate,
                    Instant.now(clock).plus(SNAPSHOT_TTL),
                    List.copyOf(quotes)
            );
            return quoteSnapshot.quotes();
        }
    }

    /**
     * 判断行情快照是否属于指定交易日且尚未过期。
     */
    private boolean isSnapshotAvailable(
            QuoteSnapshot snapshot,
            LocalDate tradeDate,
            Instant currentInstant
    ) {
        return snapshot != null
                && snapshot.tradeDate().equals(tradeDate)
                && currentInstant.isBefore(snapshot.expiresAt());
    }

    /**
     * 分批读取当天完整股票清单并获取全市场实时行情。
     */
    private List<StockDailyQuote> fetchCurrentQuotes(LocalDate tradeDate) {
        // 股票清单低于固定完整性阈值时拒绝请求和生成快照
        int expectedStockCount = stockBasicService.countSnapshot(tradeDate);
        if (expectedStockCount < MINIMUM_STOCK_COUNT) {
            throw new IllegalStateException(
                    "当天股票清单不完整，minimum=" + MINIMUM_STOCK_COUNT
                            + "，actual=" + expectedStockCount
            );
        }

        // 使用股票代码游标分批读取当天完整股票清单
        List<StockBasic> stocks = new ArrayList<>(expectedStockCount);
        String lastStockCode = "";
        while (stocks.size() < expectedStockCount) {
            List<StockBasic> batch = stockBasicService.findSnapshotBatch(
                    tradeDate,
                    lastStockCode,
                    STOCK_BATCH_SIZE
            );
            if (batch.isEmpty()) {
                break;
            }
            stocks.addAll(batch);
            lastStockCode = batch.get(batch.size() - 1).getStockCode();
        }
        if (stocks.size() != expectedStockCount) {
            throw new IllegalStateException(
                    "当天股票清单读取不完整，expected=" + expectedStockCount
                            + "，actual=" + stocks.size()
            );
        }

        // 实时行情数量必须与当天股票清单完全一致
        List<StockDailyQuote> quotes = stockDailyQuoteSourceService.fetchAll(tradeDate, stocks);
        if (quotes == null || quotes.size() != expectedStockCount) {
            int actualCount = quotes == null ? 0 : quotes.size();
            throw new IllegalStateException(
                    "实时行情快照不完整，expected=" + expectedStockCount
                            + "，actual=" + actualCount
            );
        }
        return quotes;
    }

    /**
     * 进程内全市场实时行情快照。
     */
    private record QuoteSnapshot(
            LocalDate tradeDate,
            Instant expiresAt,
            List<StockDailyQuote> quotes
    ) {
    }
}
