package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketLevelCalculator;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.pojo.dto.DailyMarketTurnoverDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
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

/**
 * 市场水位服务实现。
 */
@Service
public class MarketLevelServiceImpl implements MarketLevelService {

    private static final int HISTORY_DAYS = 5;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    private final MarketLevelSourceService marketLevelSourceService;
    private final TradeCalendarService tradeCalendarService;
    private final MarketLevelCalculator marketLevelCalculator;
    private final Clock clock;

    /**
     * 创建市场水位服务。
     *
     * @param marketDailyTurnoverMapper   已校验的每日市场成交额Mapper
     * @param marketLevelSourceService 市场水位实时数据源
     * @param tradeCalendarService     交易日历服务
     * @param marketLevelCalculator    市场水位计算组件
     */
    @Autowired
    public MarketLevelServiceImpl(
            MarketDailyTurnoverMapper marketDailyTurnoverMapper,
            MarketLevelSourceService marketLevelSourceService,
            TradeCalendarService tradeCalendarService,
            MarketLevelCalculator marketLevelCalculator
    ) {
        this(
                marketDailyTurnoverMapper,
                marketLevelSourceService,
                tradeCalendarService,
                marketLevelCalculator,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的市场水位服务，供日期边界测试使用。
     *
     * @param marketDailyTurnoverMapper    已校验的每日市场成交额Mapper
     * @param marketLevelSourceService 市场水位实时数据源
     * @param tradeCalendarService      交易日历服务
     * @param marketLevelCalculator     市场水位计算组件
     * @param clock                     日期时间时钟
     */
    MarketLevelServiceImpl(
            MarketDailyTurnoverMapper marketDailyTurnoverMapper,
            MarketLevelSourceService marketLevelSourceService,
            TradeCalendarService tradeCalendarService,
            MarketLevelCalculator marketLevelCalculator,
            Clock clock
    ) {
        this.marketDailyTurnoverMapper = marketDailyTurnoverMapper;
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
        // 与周月统计使用同一份已校验日汇总，禁止缺行明细被误当作全市场金额。
        List<DailyMarketTurnoverDto> historicalTurnovers = loadHistoricalTurnovers(historicalTradeDates);
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

    private List<DailyMarketTurnoverDto> loadHistoricalTurnovers(List<LocalDate> tradeDates) {
        List<MarketDailyTurnover> records = marketDailyTurnoverMapper.selectCompleteByDateRange(
                tradeDates.get(tradeDates.size() - 1), tradeDates.get(0));
        Map<LocalDate, MarketDailyTurnover> byDate = new java.util.HashMap<>();
        for (MarketDailyTurnover record : records) {
            if (record == null || !tradeDates.contains(record.getTradeDate())
                    || !"COMPLETE".equals(record.getDataStatus())
                    || byDate.put(record.getTradeDate(), record) != null) {
                throw new IllegalStateException("历史市场成交额汇总日期或状态无效");
            }
        }
        List<DailyMarketTurnoverDto> result = new ArrayList<>(HISTORY_DAYS);
        for (LocalDate tradeDate : tradeDates) {
            MarketDailyTurnover record = byDate.get(tradeDate);
            if (record == null || record.getStockCount() == null || record.getStockCount() <= 0
                    || !record.getStockCount().equals(record.getAmountRecordCount())
                    || record.getTurnoverAmountYuan() == null || record.getTurnoverAmountYuan().signum() < 0) {
                throw new IllegalStateException("历史市场成交额汇总缺失或不完整，tradeDate=" + tradeDate);
            }
            result.add(DailyMarketTurnoverDto.builder().tradeDate(tradeDate)
                    .turnoverAmountYuan(record.getTurnoverAmountYuan())
                    .totalRecordCount(record.getStockCount()).amountRecordCount(record.getAmountRecordCount())
                    .build());
        }
        return result;
    }
}
