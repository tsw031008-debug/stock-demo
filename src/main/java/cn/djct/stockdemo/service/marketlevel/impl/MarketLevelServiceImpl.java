package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketLevelCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.DailyMarketTurnoverDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.service.marketlevel.MarketLevelService;
import cn.djct.stockdemo.service.marketlevel.MarketLevelSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 市场水位服务实现。
 */
@Service
public class MarketLevelServiceImpl implements MarketLevelService {

    private static final int HISTORY_DAYS = 5;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final MarketLevelSourceService marketLevelSourceService;
    private final TradeCalendarService tradeCalendarService;
    private final MarketLevelCalculator marketLevelCalculator;
    private final Clock clock;

    /**
     * 创建市场水位服务。
     *
     * @param stockDailyQuoteMapper   股票日行情Mapper
     * @param marketLevelSourceService 市场水位实时数据源
     * @param tradeCalendarService     交易日历服务
     * @param marketLevelCalculator    市场水位计算组件
     */
    @Autowired
    public MarketLevelServiceImpl(
            StockDailyQuoteMapper stockDailyQuoteMapper,
            MarketLevelSourceService marketLevelSourceService,
            TradeCalendarService tradeCalendarService,
            MarketLevelCalculator marketLevelCalculator
    ) {
        this(
                stockDailyQuoteMapper,
                marketLevelSourceService,
                tradeCalendarService,
                marketLevelCalculator,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的市场水位服务，供日期边界测试使用。
     *
     * @param stockDailyQuoteMapper    股票日行情Mapper
     * @param marketLevelSourceService 市场水位实时数据源
     * @param tradeCalendarService      交易日历服务
     * @param marketLevelCalculator     市场水位计算组件
     * @param clock                     日期时间时钟
     */
    MarketLevelServiceImpl(
            StockDailyQuoteMapper stockDailyQuoteMapper,
            MarketLevelSourceService marketLevelSourceService,
            TradeCalendarService tradeCalendarService,
            MarketLevelCalculator marketLevelCalculator,
            Clock clock
    ) {
        this.stockDailyQuoteMapper = stockDailyQuoteMapper;
        this.marketLevelSourceService = marketLevelSourceService;
        this.tradeCalendarService = tradeCalendarService;
        this.marketLevelCalculator = marketLevelCalculator;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 查询并计算最新市场水位。
     *
     * @return 最新市场水位
     */
    @Override
    public MarketLevelRespVo getLatest() {
        // 使用上海时区确定当前日期和时间
        LocalDate currentDate = LocalDate.now(clock);
        LocalTime currentTime = LocalTime.now(clock);
        // 判断当天是否是交易日或者是否未开盘，不是返回上一个交易日，是则确认当天作为统计日
        LocalDate statisticsDate = resolveStatisticsDate(currentDate, currentTime);
        // 获取统计日前5个交易日，历史范围不包含当前统计日
        List<LocalDate> historicalTradeDates = resolveHistoricalTradeDates(statisticsDate);
        // 获取前5日的沪深A股的成交额
        List<MarketTurnoverRecordDto> turnoverRecords =
                stockDailyQuoteMapper.selectMarketTurnoverRecords(historicalTradeDates);
        // 按交易日汇总成交额
        List<DailyMarketTurnoverDto> historicalTurnovers =
                aggregateHistoricalTurnovers(historicalTradeDates, turnoverRecords);
        // 获取腾讯实时沪深两市成交额并计算市场水位
        BigDecimal currentTurnoverAmountYuan = marketLevelSourceService.fetchCurrentTurnoverAmountYuan();
        return marketLevelCalculator.calculate(currentTurnoverAmountYuan, historicalTurnovers);
    }

    /**
     * 确定实时成交额所属的统计交易日。
     *
     * @param currentDate 当前日期
     * @param currentTime 当前时间
     * @return 统计交易日
     */
    private LocalDate resolveStatisticsDate(LocalDate currentDate, LocalTime currentTime) {
        //当天是交易日并且已经开盘了
        if (tradeCalendarService.isTradingDay(currentDate) && !currentTime.isBefore(MARKET_OPEN_TIME)) {
            return currentDate;
        }
        //当天不是交易日，返回前一个交易日
        return tradeCalendarService.getPreviousTradingDay(currentDate, 1);
    }

    /**
     * 获取统计日前5个交易日，按日期倒序排列。
     *
     * @param statisticsDate 统计日
     * @return 统计日前5个交易日
     */
    private List<LocalDate> resolveHistoricalTradeDates(LocalDate statisticsDate) {
        List<LocalDate> tradeDates = new ArrayList<>(HISTORY_DAYS);
        for (int offset = 1; offset <= HISTORY_DAYS; offset++) {
            tradeDates.add(tradeCalendarService.getPreviousTradingDay(statisticsDate, offset));
        }
        return tradeDates;
    }

    /**
     * 按交易日汇总成交额，并统计成交额字段是否完整。
     *
     * @param tradeDates      需要汇总的交易日，按日期倒序排列
     * @param turnoverRecords 成交额原始记录
     * @return 每个交易日的成交额汇总数据
     */
    private List<DailyMarketTurnoverDto> aggregateHistoricalTurnovers(
            List<LocalDate> tradeDates,
            List<MarketTurnoverRecordDto> turnoverRecords
    ) {
        // 校验历史成交额明细不能为空
        Objects.requireNonNull(turnoverRecords, "历史市场成交额明细不能为空");
        //按照交易日进行分组统计
        Map<LocalDate, List<MarketTurnoverRecordDto>> recordsByDate = turnoverRecords.stream()
                .collect(Collectors.groupingBy(MarketTurnoverRecordDto::getTradeDate));

        //返回按交易日顺序排列的成交额汇总数据
        return tradeDates.stream()
                .map(tradeDate -> aggregateDailyTurnover(
                        tradeDate,
                        recordsByDate.getOrDefault(tradeDate, List.of())
                ))
                .toList();
    }

    /**
     * 汇总单个交易日成交额，空成交额不参与求和并由记录数差异标记为数据不完整。
     *
     * @param tradeDate 交易日
     * @param records   当日成交额原始记录
     * @return 当日成交额汇总数据
     */
    private DailyMarketTurnoverDto aggregateDailyTurnover(
            LocalDate tradeDate,
            List<MarketTurnoverRecordDto> records
    ) {
        // 求和当日所有成交额
        BigDecimal turnoverAmountYuan = records.stream()
                .map(MarketTurnoverRecordDto::getTurnoverAmountYuan)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 统计当日成交额记录数
        int amountRecordCount = (int) records.stream()
                .map(MarketTurnoverRecordDto::getTurnoverAmountYuan)
                .filter(Objects::nonNull)
                .count();

        return DailyMarketTurnoverDto.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(turnoverAmountYuan)
                .totalRecordCount(records.size())
                .amountRecordCount(amountRecordCount)
                .build();
    }
}
