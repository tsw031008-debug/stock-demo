package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexStyleCalculator;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 指数大小风格查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class IndexStyleServiceImpl implements IndexStyleService {

    private static final int TRADING_DAY_COUNT = 5;

    private final IndexDailyQuoteMapper indexDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final IndexStyleCalculator indexStyleCalculator;

    /**
     * 以本地最新指数交易日为统计日，比较最近五个完整交易日。
     */
    @Override
    public IndexStyleComparisonDto getLatest() {
        // 查最新交易日
        LocalDate statisticsDate = indexDailyQuoteMapper.selectLatestTradeDate();
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的指数日行情数据");
        }
        // 获取统计日之前的第5个交易日
        LocalDate startDate = tradeCalendarService.getPreviousTradingDay(
                statisticsDate,
                TRADING_DAY_COUNT - 1
        );
        // 获取统计日和开始日之间的所有交易日
        List<LocalDate> tradeDates = tradeCalendarService.getTradingDays(startDate, statisticsDate);
        if (tradeDates.size() != TRADING_DAY_COUNT) {
            throw new IllegalStateException("指数风格交易日数据不完整，startDate=" + startDate
                    + "，endDate=" + statisticsDate);
        }

        //获取需要统计的完整指数代码
        List<String> indexCodes = Arrays.stream(IndexStyleIndex.values())
                .map(IndexStyleIndex::getIndexCode)
                .toList();
        List<IndexDailyQuote> quotes = indexDailyQuoteMapper.selectByTradeDatesAndCodes(
                tradeDates,
                indexCodes
        );
        return IndexStyleComparisonDto.builder()
                .statisticsDate(statisticsDate)
                .tradeDates(tradeDates)
                .indices(indexStyleCalculator.calculate(tradeDates, quotes))
                .build();
    }
}
