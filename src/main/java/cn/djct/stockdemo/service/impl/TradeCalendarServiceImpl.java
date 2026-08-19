package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import cn.djct.stockdemo.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 交易日历服务实现。
 */
@Service
@RequiredArgsConstructor
public class TradeCalendarServiceImpl implements TradeCalendarService {
    //默认市场代码
    private static final String DEFAULT_MARKET_CODE = "CN_A";

    //构造器注入
    private final TradeCalendarMapper tradeCalendarMapper;

    /**
     * 判断指定日期是否为A股交易日。
     *
     * @param date
     */
    @Override
    public boolean isTradingDay(LocalDate date) {
        //非空校验
        if (date==null) {
            throw new IllegalArgumentException("日期不能为空！");
        }
        // 查询指定市场代码和交易日期的交易日历
        TradeCalendar tradeCalendar = tradeCalendarMapper.selectByMarketCodeAndTradeDate(DEFAULT_MARKET_CODE, date);
        //查询为空
        if (tradeCalendar == null) {
            throw new IllegalArgumentException("该日期不是交易日，date="+date);
        }
        return tradeCalendar.getIsTradingDay();
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
        LocalDate tradingDate = tradeCalendarMapper.selectPreviousTradingDate(DEFAULT_MARKET_CODE, date, offset-1);
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
        //查询前n个交易日
        LocalDate tradingDate = tradeCalendarMapper.selectNextTradingDate(DEFAULT_MARKET_CODE, date, offset-1);
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
}
