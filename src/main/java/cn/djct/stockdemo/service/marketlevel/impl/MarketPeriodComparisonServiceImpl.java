package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.pojo.vo.MarketPeriodComparisonRespVo;
import cn.djct.stockdemo.pojo.vo.MarketPeriodItemRespVo;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import cn.djct.stockdemo.service.marketlevel.MarketPeriodComparisonService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 市场周月平均成交额同比环比服务实现。
 */
@Service
@RequiredArgsConstructor
public class MarketPeriodComparisonServiceImpl implements MarketPeriodComparisonService {

    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月");
    private static final int MAX_PREVIOUS_WEEK_SEARCH = 8;

    private final MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    private final TradeCalendarService tradeCalendarService;

    /**
     * 查询最新完整交易日对应的周、月平均水位。
     *
     * @return 周、月同比、环比和当前周期的日均成交额
     */
    @Override
    public MarketPeriodComparisonRespVo getLatest() {
        // 以每日汇总表中最新的COMPLETE记录作为统计截止日，避免使用未收盘或残缺数据
        LocalDate statisticsDate = marketDailyTurnoverMapper.selectLatestCompleteTradeDate();
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的每日市场成交额数据");
        }

        // 周同比按需求统一向前推5个自然周；周环比取上一个实际包含交易日的自然周
        LocalDate currentWeekStart = statisticsDate.with(DayOfWeek.MONDAY);

        // 周同比向前推5个自然周，获取对应交易日和交易成交额汇总
        PeriodData weeklyYoy = loadPeriod(currentWeekStart.minusWeeks(5),
                currentWeekStart.minusWeeks(5).plusDays(6));
        // 周环比取上一个实际包含交易日的自然周
        PeriodData weeklyMom = findPreviousTradingWeek(currentWeekStart);
        // 当前周只统计周一至最新完整交易日，不固定要求5个交易日
        PeriodData weeklyCurrent = loadPeriod(currentWeekStart, statisticsDate);

        // 月同比取去年同月，月环比取上一个自然月，当前月统计至最新完整交易日
        YearMonth currentMonth = YearMonth.from(statisticsDate);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        YearMonth lastYearMonth = currentMonth.minusYears(1);
        PeriodData monthlyYoy = loadPeriod(lastYearMonth.atDay(1), lastYearMonth.atEndOfMonth());
        PeriodData monthlyMom = loadPeriod(previousMonth.atDay(1), previousMonth.atEndOfMonth());
        PeriodData monthlyCurrent = loadPeriod(currentMonth.atDay(1), statisticsDate);

        // 图表展示顺序固定为同比、环比、当前，周和月保持一致
        return MarketPeriodComparisonRespVo.builder()
                .statisticsTradeDate(statisticsDate)
                .weekly(List.of(
                        toItem("YOY", weeklyYoy, false),
                        toItem("MOM", weeklyMom, false),
                        toItem("CURRENT", weeklyCurrent, false)
                ))
                .monthly(List.of(
                        toItem("YOY", monthlyYoy, true),
                        toItem("MOM", monthlyMom, true),
                        toItem("CURRENT", monthlyCurrent, true)
                ))
                .build();
    }

    /**
     * 查找当前周之前最近一个实际包含交易日的自然周。
     * 春节等整周休市时继续向前查找，最多查找8周。
     *
     * @param currentWeekStart 当前自然周周一
     * @return 上一个实际交易周的数据
     */
    private PeriodData findPreviousTradingWeek(LocalDate currentWeekStart) {

        LocalDate weekStart = currentWeekStart.minusWeeks(1);
        // 从当前周之前第1个自然周开始向前查找，最多查找8个自然周
        for (int index = 0; index < MAX_PREVIOUS_WEEK_SEARCH; index++) {
            PeriodData period = loadPeriod(weekStart, weekStart.plusDays(6));
            // 找到实际交易日，则返回该周数据
            if (!period.tradingDays().isEmpty()) {
                return period;
            }
            // 没找到实际交易日，则继续向前查找
            weekStart = weekStart.minusWeeks(1);
        }
        throw new IllegalStateException("前8个自然周内没有可用交易周");
    }

    /**
     * 根据交易日历加载指定自然周期内的完整每日成交额。
     *
     * @param startDate 自然周期开始日期
     * @param endDate   自然周期结束日期
     * @return 周期交易日及每日成交额
     */
    private PeriodData loadPeriod(LocalDate startDate, LocalDate endDate) {
        // 交易日历决定周期内应有的数据日期
        List<LocalDate> tradingDays = tradeCalendarService.getTradingDays(startDate, endDate);
        if (tradingDays.isEmpty()) {
            return new PeriodData(startDate, endDate, tradingDays, List.of());
        }
        // 查询只读取每日汇总表，不在接口查询时扫描股票日行情明细
        List<MarketDailyTurnover> turnovers = marketDailyTurnoverMapper
                .selectCompleteByDateRange(startDate, endDate);
        // 实际记录日期集合，用于后续与交易日历对比
        Set<LocalDate> actualDates = new HashSet<>(turnovers.stream()
                .map(MarketDailyTurnover::getTradeDate)
                .toList());
        // 实际完整记录必须与交易日历逐日一致，缺少任意交易日都不计算平均值
        if (turnovers.size() != tradingDays.size() || !actualDates.equals(new HashSet<>(tradingDays))) {
            throw new IllegalStateException("市场成交额周期数据不完整，startDate=" + startDate
                    + "，endDate=" + endDate);
        }
        return new PeriodData(startDate, endDate, tradingDays, turnovers);
    }

    /**
     * 计算一个周期的日均成交额并转换为亿元。
     *
     * @param comparisonType 比较类型
     * @param period         周期数据
     * @param monthly        是否为月周期
     * @return 周期平均水位
     */
    private MarketPeriodItemRespVo toItem(String comparisonType, PeriodData period, boolean monthly) {
        // 整个自然周期没有交易日时显式返回不可用，不用0伪装成真实水位
        if (period.tradingDays().isEmpty()) {
            return MarketPeriodItemRespVo.builder()
                    .comparisonType(comparisonType)
                    .periodLabel(formatLabel(period, monthly))
                    .startDate(period.startDate())
                    .endDate(period.endDate())
                    .tradingDayCount(0)
                    .available(false)
                    .build();
        }

        // 周期内成交额总和
        BigDecimal total = period.turnovers().stream()
                .map(MarketDailyTurnover::getTurnoverAmountYuan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate actualStart = period.tradingDays().get(0);
        LocalDate actualEnd = period.tradingDays().get(period.tradingDays().size() - 1);
        return MarketPeriodItemRespVo.builder()
                .comparisonType(comparisonType)
                //返回前端对应的坐标标签
                .periodLabel(formatLabel(period, monthly))
                .startDate(actualStart)
                .endDate(actualEnd)
                .tradingDayCount(period.tradingDays().size())
                // 平均水位 = 周期内成交额总和 ÷ 实际交易日数量，再从元转换为亿元
                .averageTurnoverYi(total
                        .divide(BigDecimal.valueOf(period.tradingDays().size()), 8, RoundingMode.HALF_UP)
                        .divide(ONE_HUNDRED_MILLION, 2, RoundingMode.HALF_UP))
                .available(true)
                .build();
    }

    /**
     * 生成图表周期标签。
     * 月度显示所属月份；周度显示实际首尾交易日，
     * 周期无交易日时使用自然周期的起止日期。
     */
    private String formatLabel(PeriodData period, boolean monthly) {
        //如果是月份 直接显示月份，例如 2026年08月
        if (monthly) {
            return YearMonth.from(period.startDate()).format(MONTH_LABEL_FORMATTER);
        }
        //如果是周 显示实际首个和最后一个交易日，例如 2026-08-24~2026-08-28
        LocalDate start = period.tradingDays().isEmpty()
                ? period.startDate() : period.tradingDays().get(0);
        LocalDate end = period.tradingDays().isEmpty()
                ? period.endDate() : period.tradingDays().get(period.tradingDays().size() - 1);
        return start + "~" + end;
    }

    private record PeriodData(
            LocalDate startDate,
            LocalDate endDate,
            List<LocalDate> tradingDays,
            List<MarketDailyTurnover> turnovers
    ) {
    }
}
