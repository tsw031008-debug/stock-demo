package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.StockAlertCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.vo.StockOpenBoardAlertRespVo;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockalert.StockAlertService;
import cn.djct.stockdemo.service.stockdailyquote.StockQuoteSnapshotService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 股票预警服务实现。
 */
@Service
public class StockAlertServiceImpl implements StockAlertService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockQuoteSnapshotService stockQuoteSnapshotService;
    private final TradeCalendarService tradeCalendarService;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StockAlertCalculator stockAlertCalculator;
    private final Clock clock;

    /**
     * 创建股票预警服务。
     *
     * @param stockQuoteSnapshotService     全市场实时行情快照服务
     * @param tradeCalendarService          交易日历服务
     * @param stockDailyQuoteMapper         股票日行情Mapper
     * @param stockAlertCalculator          股票预警计算组件
     */
    @Autowired
    public StockAlertServiceImpl(
            StockQuoteSnapshotService stockQuoteSnapshotService,
            TradeCalendarService tradeCalendarService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockAlertCalculator stockAlertCalculator
    ) {
        this(
                stockQuoteSnapshotService,
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
            StockQuoteSnapshotService stockQuoteSnapshotService,
            TradeCalendarService tradeCalendarService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockAlertCalculator stockAlertCalculator,
            Clock clock
    ) {
        this.stockQuoteSnapshotService = stockQuoteSnapshotService;
        this.tradeCalendarService = tradeCalendarService;
        this.stockDailyQuoteMapper = stockDailyQuoteMapper;
        this.stockAlertCalculator = stockAlertCalculator;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 查询并计算涨速预警股票。
     */
    @Override
    public PageRespVo<StockSpeedAlertDto> findSpeedAlerts(int pageNum, int pageSize) {
        // 校验分页参数和当前是否允许查询
        validatePage(pageNum, pageSize);
        LocalDate currentDate = validateQueryTime();
        // 获取两个预警接口共享的全市场实时行情快照
        List<StockDailyQuote> currentQuotes = stockQuoteSnapshotService.getSnapshot(currentDate);
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
    public PageRespVo<StockOpenBoardAlertRespVo> findOpenBoardAlerts(int pageNum, int pageSize) {
        // 校验分页参数和当前是否允许查询
        validatePage(pageNum, pageSize);
        LocalDate currentDate = validateQueryTime();
        // 复用实时快照并完成全量筛选和排序
        List<StockOpenBoardAlertRespVo> alerts = stockAlertCalculator.calculateOpenBoardAlerts(
                stockQuoteSnapshotService.getSnapshot(currentDate)
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
     * 对已完成全量筛选和排序的数据进行分页。
     */
    private <T> PageRespVo<T> paginate(List<T> records, int pageNum, int pageSize) {
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
        return PageRespVo.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(records.size())
                .records(pageRecords)
                .build();
    }

}
