package cn.djct.stockdemo.face;

import cn.djct.stockdemo.constant.TradeDayType;
import cn.djct.stockdemo.pojo.entity.NationalHoliday;
import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 交易日历生成计算组件。
 */
@Component
public class TradeCalendarDataInitializer {

    /**
     * 根据日期范围和节假日数据生成交易日历。
     */
    public List<TradeCalendar> generate(
            String marketCode,
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, NationalHoliday> holidayMap
    ) {
        Objects.requireNonNull(marketCode, "市场代码不能为空");
        Objects.requireNonNull(startDate, "开始日期不能为空");
        Objects.requireNonNull(endDate, "结束日期不能为空");
        Objects.requireNonNull(holidayMap, "节假日数据不能为空");

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        //查询起始日期到结束日期的天数，包含起止日期
        int dayCount = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
        List<TradeCalendar> calendars = new ArrayList<>(dayCount);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            calendars.add(buildTradeCalendar(marketCode, currentDate, holidayMap.get(currentDate)));
            currentDate = currentDate.plusDays(1);
        }

        return calendars;
    }

    //创建交易日实体
    private TradeCalendar buildTradeCalendar(
            String marketCode,
            LocalDate date,
            NationalHoliday holiday
    ) {
        TradeCalendar.TradeCalendarBuilder builder = TradeCalendar.builder()
                .marketCode(marketCode)
                .tradeDate(date)
                .isManualAdjusted(false);

        //当前日期为节假日
        if (holiday != null) {
            return builder
                    .isTradingDay(false)
                    .dayType(TradeDayType.NATIONAL_HOLIDAY)
                    .holidayName(holiday.getHolidayName())
                    .sourceTitle(holiday.getSourceTitle())
                    .sourceUrl(holiday.getSourceUrl())
                    .remark(holiday.getRemark())
                    .build();
        }

        //当前日期为周末
        if (isWeekend(date)) {
            return builder
                    .isTradingDay(false)
                    .dayType(TradeDayType.WEEKEND)
                    .remark("周末休市")
                    .build();
        }

        return builder
                .isTradingDay(true)
                .dayType(TradeDayType.TRADING_DAY)
                .build();
    }

    //判断是否为周末
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
