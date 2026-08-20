package cn.djct.stockdemo.service;

import java.time.LocalDate;

/**
 * 交易日历服务。
 */
public interface TradeCalendarService {

    /**
     * 初始化指定日期范围内的A股交易日历。
     *
     * @return 实际新增数量
     */
    int initialize(LocalDate startDate, LocalDate endDate);

    /**
     * 判断指定日期是否为A股交易日。
     */
    boolean isTradingDay(LocalDate date);

    /**
     * 查询指定日期之前的第N个交易日。
     *
     * @param date   基准日期
     * @param offset 偏移数量，从1开始
     */
    LocalDate getPreviousTradingDay(LocalDate date, int offset);

    /**
     * 查询指定日期之后的第N个交易日。
     *
     * @param date   基准日期
     * @param offset 偏移数量，从1开始
     */
    LocalDate getNextTradingDay(LocalDate date, int offset);
}
