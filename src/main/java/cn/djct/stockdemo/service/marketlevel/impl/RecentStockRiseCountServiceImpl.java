package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.RecentStockRiseCountCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.RecentStockRiseCountDto;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.service.marketlevel.RecentStockRiseCountService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 近期股票涨幅家数查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class RecentStockRiseCountServiceImpl implements RecentStockRiseCountService {

    private static final int REQUIRED_TRADING_DAY_COUNT = 20;
    private static final int DISPLAY_TRADING_DAY_COUNT = 10;
    private static final int EARLIEST_TRADING_DAY_OFFSET = 19;
    private static final int MINIMUM_STOCK_COUNT = 3000;

    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final RecentStockRiseCountCalculator calculator;

    /**
     * 使用最新股票日行情统计最近10个交易日的股票涨幅家数。
     */
    @Override
    public List<RecentStockRiseCountDto> getLatest() {
        // 以日行情表最新交易日作为统计截止日，盘中未落库行情不会参与统计
        LocalDate statisticsDate = stockDailyQuoteMapper.selectLatestTradeDate();
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的股票日行情数据");
        }

        // 10个展示日中最早一天还需要前第10个交易日，因此共读取20个交易日
        LocalDate startDate = tradeCalendarService.getPreviousTradingDay(
                statisticsDate,
                EARLIEST_TRADING_DAY_OFFSET
        );
        List<LocalDate> tradingDates = tradeCalendarService.getTradingDays(
                startDate,
                statisticsDate
        );
        validateTradingDates(tradingDates, startDate, statisticsDate);

        // 一次批量读取计算所需的原始收盘价，避免按股票循环查询
        List<StockClosePriceDto> closePrices = stockDailyQuoteMapper
                .selectClosePricesByTradeDates(tradingDates);
        validateTargetTradeDates(tradingDates, closePrices);
        return calculator.calculate(tradingDates, closePrices);
    }

    private void validateTradingDates(
            List<LocalDate> tradingDates,
            LocalDate startDate,
            LocalDate statisticsDate
    ) {
        if (tradingDates == null || tradingDates.size() != REQUIRED_TRADING_DAY_COUNT
                || !startDate.equals(tradingDates.get(0))
                || !statisticsDate.equals(tradingDates.get(tradingDates.size() - 1))) {
            int actualCount = tradingDates == null ? 0 : tradingDates.size();
            throw new IllegalStateException("交易日历数据不完整，expected=20，actual=" + actualCount);
        }
    }

    private void validateTargetTradeDates(
            List<LocalDate> tradingDates,
            List<StockClosePriceDto> closePrices
    ) {
        if (closePrices == null) {
            throw new IllegalStateException("股票日行情数据不能为空");
        }
        Map<LocalDate, Integer> countByTradeDate = new HashMap<>();
        for (StockClosePriceDto closePrice : closePrices) {
            if (closePrice != null && closePrice.getTradeDate() != null) {
                countByTradeDate.merge(closePrice.getTradeDate(), 1, Integer::sum);
            }
        }

        List<LocalDate> targetDates = tradingDates.subList(
                tradingDates.size() - DISPLAY_TRADING_DAY_COUNT,
                tradingDates.size()
        );
        for (LocalDate targetDate : targetDates) {
            int actualCount = countByTradeDate.getOrDefault(targetDate, 0);
            if (actualCount < MINIMUM_STOCK_COUNT) {
                throw new IllegalStateException("股票日行情不完整，tradeDate=" + targetDate
                        + "，minimum=" + MINIMUM_STOCK_COUNT + "，actual=" + actualCount);
            }
        }
    }
}
