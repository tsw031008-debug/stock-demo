package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.RecentStockRiseCountDto;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 近期股票涨幅家数计算组件。
 */
@Component
public class RecentStockRiseCountCalculator {

    private static final int REQUIRED_TRADING_DAY_COUNT = 20;
    private static final int DISPLAY_TRADING_DAY_COUNT = 10;
    private static final int FIVE_DAY_OFFSET = 5;
    private static final int TEN_DAY_OFFSET = 10;
    private static final BigDecimal FIVE_PERCENT_MULTIPLIER = new BigDecimal("1.05");

    /**
     * 统计最近10个交易日中5日和10日涨幅严格大于5%的股票家数。
     *
     * @param tradingDates 按升序排列的最近20个交易日
     * @param closePrices  20个交易日的股票收盘价原始数据
     * @return 按交易日升序排列的最近10日统计结果
     */
    public List<RecentStockRiseCountDto> calculate(
            List<LocalDate> tradingDates,
            List<StockClosePriceDto> closePrices
    ) {
        validateTradingDates(tradingDates);
        if (closePrices == null) {
            throw new IllegalArgumentException("股票收盘价不能为空");
        }

        Set<LocalDate> tradingDateSet = new HashSet<>(tradingDates);
        Map<String, Map<LocalDate, BigDecimal>> priceByStockCode = new HashMap<>();
        for (StockClosePriceDto closePrice : closePrices) {
            validateClosePrice(closePrice, tradingDateSet);
            Map<LocalDate, BigDecimal> priceByDate = priceByStockCode.computeIfAbsent(
                    closePrice.getStockCode(),
                    ignored -> new HashMap<>()
            );
            if (priceByDate.put(closePrice.getTradeDate(), closePrice.getClosePrice()) != null) {
                throw new IllegalStateException("股票收盘价重复，stockCode="
                        + closePrice.getStockCode() + "，tradeDate=" + closePrice.getTradeDate());
            }
        }

        List<RecentStockRiseCountDto> result = new ArrayList<>(DISPLAY_TRADING_DAY_COUNT);
        for (int index = TEN_DAY_OFFSET; index < tradingDates.size(); index++) {
            LocalDate tradeDate = tradingDates.get(index);
            LocalDate fiveDayBaseDate = tradingDates.get(index - FIVE_DAY_OFFSET);
            LocalDate tenDayBaseDate = tradingDates.get(index - TEN_DAY_OFFSET);
            int fiveDayRiseCount = 0;
            int tenDayRiseCount = 0;

            // 计算5日和10日涨幅严格大于5%的股票家数
            for (Map<LocalDate, BigDecimal> priceByDate : priceByStockCode.values()) {
                BigDecimal currentClose = priceByDate.get(tradeDate);
                if (currentClose == null) {
                    continue;
                }
                //5日涨幅
                BigDecimal fiveDayBaseClose = priceByDate.get(fiveDayBaseDate);
                // 当前收盘价/前N日收盘价-1>0.05 ---> 当前收盘价>前N日收盘价*1.05
                if (isRiseOverFivePercent(currentClose, fiveDayBaseClose)) {
                    fiveDayRiseCount++;
                }

                //10日涨幅
                BigDecimal tenDayBaseClose = priceByDate.get(tenDayBaseDate);
                // 当前收盘价/前N日收盘价-1>0.05 ---> 当前收盘价>前N日收盘价*1.05
                if (isRiseOverFivePercent(currentClose, tenDayBaseClose)) {
                    tenDayRiseCount++;
                }
            }

            result.add(RecentStockRiseCountDto.builder()
                    .tradeDate(tradeDate)
                    .fiveDayRiseCount(fiveDayRiseCount)
                    .tenDayRiseCount(tenDayRiseCount)
                    .build());
        }
        return result;
    }

    // 当前收盘价/前N日收盘价-1>0.05 ---> 当前收盘价>前N日收盘价*1.05
    private boolean isRiseOverFivePercent(BigDecimal currentClose, BigDecimal baseClose) {
        return baseClose != null
                && currentClose.compareTo(baseClose.multiply(FIVE_PERCENT_MULTIPLIER)) > 0;
    }

    private void validateTradingDates(List<LocalDate> tradingDates) {
        if (tradingDates == null || tradingDates.size() != REQUIRED_TRADING_DAY_COUNT) {
            int actualCount = tradingDates == null ? 0 : tradingDates.size();
            throw new IllegalArgumentException("计算需要20个交易日，actual=" + actualCount);
        }
        for (int index = 0; index < tradingDates.size(); index++) {
            LocalDate tradeDate = tradingDates.get(index);
            if (tradeDate == null) {
                throw new IllegalArgumentException("交易日不能为空");
            }
            if (index > 0 && !tradingDates.get(index - 1).isBefore(tradeDate)) {
                throw new IllegalArgumentException("交易日必须按升序排列且不能重复");
            }
        }
    }

    private void validateClosePrice(
            StockClosePriceDto closePrice,
            Set<LocalDate> tradingDates
    ) {
        if (closePrice == null || closePrice.getStockCode() == null
                || closePrice.getStockCode().isBlank() || closePrice.getTradeDate() == null
                || closePrice.getClosePrice() == null) {
            throw new IllegalStateException("股票收盘价字段不完整");
        }
        if (!tradingDates.contains(closePrice.getTradeDate())) {
            throw new IllegalStateException("股票收盘价日期超出计算范围，tradeDate="
                    + closePrice.getTradeDate());
        }
        if (closePrice.getClosePrice().signum() <= 0) {
            throw new IllegalStateException("股票收盘价必须大于0，stockCode="
                    + closePrice.getStockCode() + "，tradeDate=" + closePrice.getTradeDate());
        }
    }
}
