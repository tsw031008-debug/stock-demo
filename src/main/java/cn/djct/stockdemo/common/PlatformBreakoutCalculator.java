package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 按未复权日线判断平台突破，输入为交易日历对齐的200日升序行情。 */
@Component
public class PlatformBreakoutCalculator {

    /**
     * 判断四项条件是否同时满足；调用方负责历史完整性和ST、停牌过滤。
     * 均线、最近10日和200日窗口均包含今日；平台区间T-25至T-5含两端。
     */
    public boolean matches(List<StockDailyQuote> quotes) {
        if (quotes.size() != 200) {
            throw new IllegalArgumentException("平台突破需要完整200个交易日行情");
        }
        // 数据验证
        for (StockDailyQuote quote : quotes) {
            if (quote == null || quote.getClosePrice() == null || quote.getClosePrice().signum() <= 0
                    || quote.getHighPrice() == null
                    || quote.getHighPrice().compareTo(quote.getClosePrice()) < 0) {
                throw new IllegalStateException("平台突破历史价格缺失或无效");
            }
        }
        //由于行情日期是从T-199到T升序排列
        // CON1 : MA30 > MA60
        // MA30 > MA60等价于30日总和×2 > 60日总和，避免均线舍入改变边界。
        if (sumClose(quotes, 170, 200).multiply(BigDecimal.valueOf(2))
                .compareTo(sumClose(quotes, 140, 200)) <= 0) {
            return false;
        }
        //CON2:最近存在平台调整，最近10日存在收盘价小于10日均线
        boolean adjusted = false;
        for (int day = 190; day < 200; day++) {
            //存在
            if (quotes.get(day).getClosePrice().multiply(BigDecimal.TEN)
                    .compareTo(sumClose(quotes, day - 9, day + 1)) < 0) {
                adjusted = true;
                break;
            }
        }
        //条件二不满足
        if (!adjusted) {
            return false;
        }
        //CON3:收盘突破20日平台
        //量化条件：收盘价上穿最近20日的次高点。
        //找次高点：取前25日~前5日之间的最高价
        BigDecimal platform = highest(quotes, 174, 195);
        BigDecimal close = quotes.get(199).getClosePrice();
        //上穿：昨收小于次高点，今收大于次高点
        if (quotes.get(198).getClosePrice().compareTo(platform) >= 0
                || close.compareTo(platform) <= 0) {
            return false;
        }
        //CON4:收盘价在200日最高价附近
        //量化条件：收盘价在最近200日最高价上下10%之间。
        BigDecimal highest200 = highest(quotes, 0, 200);
        return close.compareTo(highest200.multiply(new BigDecimal("0.90"))) >= 0
                && close.compareTo(highest200.multiply(new BigDecimal("1.10"))) <= 0;
    }

    private BigDecimal sumClose(List<StockDailyQuote> quotes, int start, int end) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int index = start; index < end; index++) {
            sum = sum.add(quotes.get(index).getClosePrice());
        }
        return sum;
    }

    private BigDecimal highest(List<StockDailyQuote> quotes, int start, int end) {
        BigDecimal highest = BigDecimal.ZERO;
        for (int index = start; index < end; index++) {
            highest = highest.max(quotes.get(index).getHighPrice());
        }
        return highest;
    }
}
