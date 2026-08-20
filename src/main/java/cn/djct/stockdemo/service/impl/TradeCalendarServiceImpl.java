package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.face.TradeCalendarDataInitializer;
import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.entity.NationalHoliday;
import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import cn.djct.stockdemo.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 交易日历服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCalendarServiceImpl implements TradeCalendarService {

    private static final String DEFAULT_MARKET_CODE = "CN_A";

    private static final int BATCH_SIZE = 500;

    private final TradeCalendarMapper tradeCalendarMapper;

    private final NationalHolidayMapper nationalHolidayMapper;

    private final TradeCalendarDataInitializer tradeCalendarDataInitializer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initialize(LocalDate startDate, LocalDate endDate) {
        //参数校验
        validateDateRange(startDate, endDate);

        //统计指定日期范围是否已经存在交易日历数据，如果存在则抛出异常
        int existingCount = tradeCalendarMapper.countByMarketCodeAndDateRange(
                DEFAULT_MARKET_CODE,
                startDate,
                endDate
        );
        if (existingCount > 0) {
            throw new IllegalStateException(
                    "指定日期范围已经存在交易日历，existingCount=" + existingCount
            );
        }

        //未查询到交易日数据，进行初始化
        //查询范围内的节假日日期
        List<NationalHoliday> holidays = nationalHolidayMapper.selectByDateRange(startDate, endDate);
        Map<LocalDate, NationalHoliday> holidayMap = new HashMap<>(holidays.size());
        //将查询到的节假日用map存储
        for (NationalHoliday holiday : holidays) {
            holidayMap.put(holiday.getHolidayDate(), holiday);
        }

        //生成交易日历数据，包含交易日和非交易日
        List<TradeCalendar> calendars = tradeCalendarDataInitializer.generate(
                DEFAULT_MARKET_CODE,
                startDate,
                endDate,
                holidayMap
        );

        //批量新增交易日历数据
        int insertedCount = 0;
        for (int startIndex = 0; startIndex < calendars.size(); startIndex += BATCH_SIZE) {
            int endIndex = Math.min(startIndex + BATCH_SIZE, calendars.size());
            List<TradeCalendar> batch = new ArrayList<>(calendars.subList(startIndex, endIndex));
            int affectedRows = tradeCalendarMapper.insertBatch(batch);
            if (affectedRows != batch.size()) {
                throw new IllegalStateException(
                        "交易日历批量新增数量不正确，预期=" + batch.size() + "，实际=" + affectedRows
                );
            }
            insertedCount += affectedRows;
        }

        log.info(
                "交易日历初始化完成，marketCode={}，startDate={}，endDate={}，insertedCount={}",
                DEFAULT_MARKET_CODE,
                startDate,
                endDate,
                insertedCount
        );
        return insertedCount;
    }

    @Override
    public boolean isTradingDay(LocalDate date) {
        //参数校验
        Objects.requireNonNull(date, "日期不能为空");

        //查询交易日历数据
        TradeCalendar tradeCalendar = tradeCalendarMapper.selectByMarketCodeAndTradeDate(DEFAULT_MARKET_CODE, date);
        if (tradeCalendar == null) {
            throw new IllegalStateException("交易日历数据不存在，date=" + date);
        }

        return Boolean.TRUE.equals(tradeCalendar.getIsTradingDay());
    }

    /**
     * 查询指定日期之前的第N个交易日。
     *
     * @param date   基准日期
     * @param offset 偏移数量，从1开始
     */
    @Override
    public LocalDate getPreviousTradingDay(LocalDate date, int offset) {
        //参数校验
        validateParameters(date, offset);
        //查询前n个交易日
        LocalDate tradingDate = tradeCalendarMapper.selectPreviousTradingDate(DEFAULT_MARKET_CODE, date, offset - 1);
        if (tradingDate == null) {
            throw new IllegalArgumentException("未找到前" + offset + "个交易日，date=" + date);
        }
        return tradingDate;
    }

    /**
     * 查询指定日期之后的第N个交易日。
     *
     * @param date   基准日期
     * @param offset 偏移数量，从1开始
     */
    @Override
    public LocalDate getNextTradingDay(LocalDate date, int offset) {
        //参数校验
        validateParameters(date, offset);
        //查询后n个交易日
        LocalDate tradingDate = tradeCalendarMapper.selectNextTradingDate(DEFAULT_MARKET_CODE, date, offset - 1);
        if (tradingDate == null) {
            throw new IllegalArgumentException("未找到后" + offset + "个交易日，date=" + date);
        }
        return tradingDate;
    }

    /**
     * 参数校验
     * @param date 日期
     * @param offset 偏移量
     */
    private void validateParameters(LocalDate date, int offset) {
        Objects.requireNonNull(date, "日期不能为空");

        if (offset < 1) {
            throw new IllegalArgumentException(
                    "交易日偏移量必须大于等于1"
            );
        }
    }

    //日期范围校验
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "开始日期不能为空");
        Objects.requireNonNull(endDate, "结束日期不能为空");

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }
}
