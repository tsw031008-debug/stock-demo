package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.common.StockPlateCalculator;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockPlateLimitUpRespVo;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.plate.StockPlateQueryService;
import cn.djct.stockdemo.service.plate.StockPlateService;
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
 * 股票板块查询服务实现。
 */
@Service
public class StockPlateQueryServiceImpl implements StockPlateQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final TradeCalendarService tradeCalendarService;
    private final StockPlateService stockPlateService;
    private final StockQuoteSnapshotService stockQuoteSnapshotService;
    private final StockPlateCalculator stockPlateCalculator;
    private final Clock clock;

    /**
     * 创建使用系统时钟的板块查询服务。
     */
    @Autowired
    public StockPlateQueryServiceImpl(
            TradeCalendarService tradeCalendarService,
            StockPlateService stockPlateService,
            StockQuoteSnapshotService stockQuoteSnapshotService,
            StockPlateCalculator stockPlateCalculator
    ) {
        this(
                tradeCalendarService,
                stockPlateService,
                stockQuoteSnapshotService,
                stockPlateCalculator,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的板块查询服务，供交易时间边界测试使用。
     */
    StockPlateQueryServiceImpl(
            TradeCalendarService tradeCalendarService,
            StockPlateService stockPlateService,
            StockQuoteSnapshotService stockQuoteSnapshotService,
            StockPlateCalculator stockPlateCalculator,
            Clock clock
    ) {
        this.tradeCalendarService = tradeCalendarService;
        this.stockPlateService = stockPlateService;
        this.stockQuoteSnapshotService = stockQuoteSnapshotService;
        this.stockPlateCalculator = stockPlateCalculator;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 分页查询当天最新板块涨停统计。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 板块涨停统计分页数据
     */
    @Override
    public PageRespVo<StockPlateLimitUpRespVo> findLimitUpStatistics(int pageNum, int pageSize) {
        // 先校验分页和交易时间，非法请求不读取板块或请求实时行情
        validatePage(pageNum, pageSize);
        LocalDate tradeDate = validateQueryTime();
        // 获取所有板块成分
        List<StockPlateMemberDto> members = stockPlateService.findActiveMembers();
        if (members.isEmpty()) {
            throw new IllegalStateException("当前没有有效板块成分关系");
        }

        // 复用全市场60秒行情快照，完成全量计算和排序后再分页
        List<StockDailyQuote> currentQuotes = stockQuoteSnapshotService.getSnapshot(tradeDate);
        List<StockPlateLimitUpRespVo> statistics = stockPlateCalculator.calculateLimitUpStatistics(
                members,
                currentQuotes
        );
        return paginate(statistics, pageNum, pageSize);
    }

    /**
     * 校验页码和单页大小。
     */
    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
    }

    /**
     * 校验当天为交易日且已经开盘。
     */
    private LocalDate validateQueryTime() {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalStateException("当前为非交易日");
        }
        if (LocalTime.now(clock).isBefore(MARKET_OPEN_TIME)) {
            throw new IllegalStateException("当前不在交易时间");
        }
        return tradeDate;
    }

    /**
     * 对已完成全量排序的板块统计进行分页。
     */
    private PageRespVo<StockPlateLimitUpRespVo> paginate(
            List<StockPlateLimitUpRespVo> records,
            int pageNum,
            int pageSize
    ) {
        long start = (long) (pageNum - 1) * pageSize;
        List<StockPlateLimitUpRespVo> pageRecords;
        if (start >= records.size()) {
            pageRecords = List.of();
        } else {
            int end = (int) Math.min(start + pageSize, records.size());
            pageRecords = records.subList((int) start, end);
        }
        return PageRespVo.<StockPlateLimitUpRespVo>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(records.size())
                .records(pageRecords)
                .build();
    }
}
