package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexEtfChangeCalculator;
import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexEtfComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexEtfService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 指数ETF涨幅查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class IndexEtfServiceImpl implements IndexEtfService {

    private static final int TRADING_DAY_OFFSET = 5;

    private final IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final IndexEtfChangeCalculator indexEtfChangeCalculator;

    /**
     * 使用最新完整日行情计算四只固定ETF的5日涨幅。
     */
    @Override
    public IndexEtfComparisonDto getLatest() {
        List<String> etfCodes = Arrays.stream(IndexEtf.values())
                .map(IndexEtf::getEtfCode)
                .toList();
        // 获取最新日期，盘中尚未落库的当日行情不会成为统计日
        LocalDate statisticsDate = indexEtfDailyQuoteMapper.selectLatestTradeDate();
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的指数ETF日行情数据");
        }
        // 按N日涨幅公式，5日涨幅使用统计日收盘价与前第5个交易日收盘价。
        LocalDate baseTradeDate = tradeCalendarService.getPreviousTradingDay(
                statisticsDate,
                TRADING_DAY_OFFSET
        );
        List<IndexEtfDailyQuote> quotes = indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(baseTradeDate, statisticsDate),
                etfCodes
        );
        return IndexEtfComparisonDto.builder()
                .statisticsDate(statisticsDate)
                .baseTradeDate(baseTradeDate)
                .etfs(indexEtfChangeCalculator.calculate(
                        baseTradeDate,
                        statisticsDate,
                        quotes
                ))
                .build();
    }
}
