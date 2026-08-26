package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.StockAlertCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockalert.StockAlertService;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 股票预警服务实现。
 */
@Service
public class StockAlertServiceImpl implements StockAlertService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int STOCK_BATCH_SIZE = 1000;
    private static final int MINIMUM_STOCK_COUNT = 3000;
    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(60);
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockBasicService stockBasicService;
    private final StockDailyQuoteSourceService stockDailyQuoteSourceService;
    private final TradeCalendarService tradeCalendarService;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StockAlertCalculator stockAlertCalculator;
    private final Clock clock;
    private final Object snapshotLock = new Object();

    // 当前交易日行情快照
    private volatile QuoteSnapshot quoteSnapshot;

    /**
     * 创建股票预警服务。
     *
     * @param stockBasicService             股票基础信息服务
     * @param stockDailyQuoteSourceService  股票实时行情数据源
     * @param tradeCalendarService          交易日历服务
     * @param stockDailyQuoteMapper         股票日行情Mapper
     * @param stockAlertCalculator          股票预警计算组件
     */
    @Autowired
    public StockAlertServiceImpl(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            TradeCalendarService tradeCalendarService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockAlertCalculator stockAlertCalculator
    ) {
        this(
                stockBasicService,
                stockDailyQuoteSourceService,
                tradeCalendarService,
                stockDailyQuoteMapper,
                stockAlertCalculator,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的股票预警服务，供交易时间和缓存边界测试使用。
     */
    StockAlertServiceImpl(
            StockBasicService stockBasicService,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            TradeCalendarService tradeCalendarService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockAlertCalculator stockAlertCalculator,
            Clock clock
    ) {
        this.stockBasicService = stockBasicService;
        this.stockDailyQuoteSourceService = stockDailyQuoteSourceService;
        this.tradeCalendarService = tradeCalendarService;
        this.stockDailyQuoteMapper = stockDailyQuoteMapper;
        this.stockAlertCalculator = stockAlertCalculator;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 查询并计算涨速预警股票。
     */
    @Override
    public PageDto<StockSpeedAlertDto> findSpeedAlerts(int pageNum, int pageSize) {
        // 校验分页参数和当前是否允许查询
        validatePage(pageNum, pageSize);
        LocalDate currentDate = validateQueryTime();
        // 获取两个预警接口共享的全市场实时行情快照
        List<StockDailyQuote> currentQuotes = getCurrentQuoteSnapshot(currentDate);
        // 使用交易日历确定T-1和T-2，不能用自然日直接减天数
        LocalDate previousDate = tradeCalendarService.getPreviousTradingDay(currentDate, 1);
        LocalDate twoDaysAgo = tradeCalendarService.getPreviousTradingDay(currentDate, 2);
        // 查询计算所需的历史收盘价原始数据
        List<StockClosePriceDto> closePrices = stockDailyQuoteMapper.selectClosePricesByTradeDates(
                List.of(previousDate, twoDaysAgo)
        );
        // 完成全量计算、筛选和排序后再分页
        List<StockSpeedAlertDto> alerts = stockAlertCalculator.calculateSpeedAlerts(
                currentQuotes,
                closePrices,
                previousDate,
                twoDaysAgo
        );
        return paginate(alerts, pageNum, pageSize);
    }

    /**
     * 查询并计算开板提醒股票。
     */
    @Override
    public PageDto<StockOpenBoardAlertDto> findOpenBoardAlerts(int pageNum, int pageSize) {
        // 校验分页参数和当前是否允许查询
        validatePage(pageNum, pageSize);
        LocalDate currentDate = validateQueryTime();
        // 复用实时快照并完成全量筛选和排序
        List<StockOpenBoardAlertDto> alerts = stockAlertCalculator.calculateOpenBoardAlerts(
                getCurrentQuoteSnapshot(currentDate)
        );
        return paginate(alerts, pageNum, pageSize);
    }

    /**
     * 校验页码和单页大小。
     */
    private void validatePage(int pageNum, int pageSize) {
        // 页码从1开始
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        // 限制单页最大数量，避免一次返回过多数据
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
    }

    /**
     * 校验当前日期是否为交易日且当天已经开盘。
     * 交易日午休和收盘后允许查询最近行情状态。
     */
    private LocalDate validateQueryTime() {
        // 使用上海时区时钟确定当前日期
        LocalDate currentDate = LocalDate.now(clock);
        // 交易日历是判断是否交易日的最终依据
        if (!tradeCalendarService.isTradingDay(currentDate)) {
            throw new IllegalStateException("当前为非交易日");
        }
        // 开盘前没有当天行情，午休和收盘后则允许查询最近状态
        LocalTime currentTime = LocalTime.now(clock);
        if (currentTime.isBefore(MARKET_OPEN_TIME)) {
            throw new IllegalStateException("当前不在交易时间");
        }
        return currentDate;
    }

    /**
     * 获取两个接口共享的60秒全市场实时行情快照。
     */
    private List<StockDailyQuote> getCurrentQuoteSnapshot(LocalDate tradeDate) {
        // 获取当前时间
        Instant now = Instant.now(clock);
        // 获取当前快照，可能为空
        QuoteSnapshot currentSnapshot = quoteSnapshot;
        // 判断快照是否可用，可用则返回快照数据
        if (isSnapshotAvailable(currentSnapshot, tradeDate, now)) {
            return currentSnapshot.quotes();
        }
        // 快照失效时加锁，保证同一时刻只有一个线程请求全市场行情，避免重复刷新
        synchronized (snapshotLock) {
            // 获取锁后再次检查，避免等待期间其他线程已完成刷新，确保线程安全
            currentSnapshot = quoteSnapshot;
            now = Instant.now(clock);
            if (isSnapshotAvailable(currentSnapshot, tradeDate, now)) {
                return currentSnapshot.quotes();
            }
            // 刷新失败时直接抛出异常，不继续使用过期快照，确保数据新鲜
            List<StockDailyQuote> quotes = fetchCurrentQuotes(tradeDate);
            // 刷新快照，获取最新行情数据
            quoteSnapshot = new QuoteSnapshot(
                    tradeDate,
                    Instant.now(clock).plus(SNAPSHOT_TTL),
                    List.copyOf(quotes)
            );
            return quoteSnapshot.quotes();
        }
    }

    /**
     * 判断行情快照是否属于当天且尚未过期。
     */
    private boolean isSnapshotAvailable(
            QuoteSnapshot snapshot,
            LocalDate tradeDate,
            Instant currentInstant
    ) {
        // 快照必须属于当前交易日且当前时间早于过期时间
        return snapshot != null
                && snapshot.tradeDate().equals(tradeDate)
                && currentInstant.isBefore(snapshot.expiresAt());
    }

    /**
     * 分批读取当天完整股票清单并获取实时行情。
     */
    private List<StockDailyQuote> fetchCurrentQuotes(LocalDate tradeDate) {
        // 查询当天股票清单数量 并校验最低完整性阈值
        int expectedStockCount = stockBasicService.countSnapshot(tradeDate);
        if (expectedStockCount < MINIMUM_STOCK_COUNT) {
            throw new IllegalStateException(
                    "当天股票清单不完整，minimum=" + MINIMUM_STOCK_COUNT
                            + "，actual=" + expectedStockCount
            );
        }
        // 按股票代码游标分批读取当天完整股票清单
        List<StockBasic> stocks = new ArrayList<>(expectedStockCount);
        String lastStockCode = "";
        // 存储股票清单，没有到达额度
        while (stocks.size() < expectedStockCount) {
            List<StockBasic> batch = stockBasicService.findSnapshotBatch(
                    tradeDate,
                    lastStockCode,
                    STOCK_BATCH_SIZE
            );
            // 如果没有更多数据则退出循环
            if (batch.isEmpty()) {
                break;
            }
            // 存储股票清单，没有到达额度
            stocks.addAll(batch);
            // 更新游标
            lastStockCode = batch.get(batch.size() - 1).getStockCode();
        }
        // 实际读取数量必须与统计数量完全一致
        if (stocks.size() != expectedStockCount) {
            throw new IllegalStateException(
                    "当天股票清单读取不完整，expected=" + expectedStockCount
                            + "，actual=" + stocks.size()
            );
        }

        // 获取实时行情
        List<StockDailyQuote> quotes = stockDailyQuoteSourceService.fetchAll(tradeDate, stocks);
        // 行情数量不完整时整次刷新失败，不生成残缺快照
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
     * 对已完成全量筛选和排序的数据进行分页。
     */
    private <T> PageDto<T> paginate(List<T> records, int pageNum, int pageSize) {
        // 根据页码计算全量结果中的起始位置
        long start = (long) (pageNum - 1) * pageSize;
        List<T> pageRecords;
        // 超出最后一页时返回空记录并保留总数
        if (start >= records.size()) {
            pageRecords = List.of();
        } else {
            int end = (int) Math.min(start + pageSize, records.size());
            pageRecords = records.subList((int) start, end);
        }
        // 封装业务分页数据
        return PageDto.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(records.size())
                .records(pageRecords)
                .build();
    }

    /**
     * 进程内实时行情快照。
     */
    private record QuoteSnapshot(
            // 交易日
            LocalDate tradeDate,
            // 过期时间
            Instant expiresAt,
            // 行情数据
            List<StockDailyQuote> quotes
    ) {
    }
}
