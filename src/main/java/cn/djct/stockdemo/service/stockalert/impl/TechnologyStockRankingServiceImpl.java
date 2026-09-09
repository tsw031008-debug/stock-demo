package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.TechnologyStockRankingCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.TechnologyStockQuoteDto;
import cn.djct.stockdemo.pojo.dto.TechnologyStockRankingDto;
import cn.djct.stockdemo.service.stockalert.TechnologyStockPoolSourceService;
import cn.djct.stockdemo.service.stockalert.TechnologyStockRankingService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 热门和潜力科技股榜单查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class TechnologyStockRankingServiceImpl implements TechnologyStockRankingService {

    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final TechnologyStockPoolSourceService stockPoolSourceService;
    private final TechnologyStockRankingCalculator calculator;

    /**
     * 使用日行情表最新交易日查询热门和潜力科技股前五名。
     */
    @Override
    public TechnologyStockRankingDto getLatest() {
        // 获取日行情表最新交易日
        LocalDate statisticsDate = stockDailyQuoteMapper.selectLatestTradeDate();
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的股票日行情数据");
        }

        // 获取统计日5、10、15、20、60个交易日基础日
        LocalDate fiveDayBaseDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 5);
        LocalDate tenDayBaseDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 10);
        LocalDate fifteenDayBaseDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 15);
        LocalDate twentyDayBaseDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 20);
        LocalDate sixtyDayBaseDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 60);
        //获取同花顺-科技的成分股
        List<String> stockCodes = stockPoolSourceService.fetchStockCodes();
        if (stockCodes == null || stockCodes.isEmpty()) {
            throw new IllegalStateException("科技股候选池数据为空");
        }

        List<LocalDate> targetDates = List.of(
                statisticsDate,
                fiveDayBaseDate,
                tenDayBaseDate,
                fifteenDayBaseDate,
                twentyDayBaseDate,
                sixtyDayBaseDate
        );
        //查询所有成分股的各个交易日的行情数据
        List<TechnologyStockQuoteDto> quotes = stockDailyQuoteMapper
                .selectTechnologyStockQuotes(stockCodes, targetDates);
        return calculator.calculate(
                statisticsDate,
                fiveDayBaseDate,
                tenDayBaseDate,
                fifteenDayBaseDate,
                twentyDayBaseDate,
                sixtyDayBaseDate,
                quotes
        );
    }
}
