package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.StockIndexDifferenceCalculator;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceRespVo;
import cn.djct.stockdemo.service.indexstyle.StockIndexDifferenceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** 只查询指定两交易日已落库收盘价，不触发外部采集或回退日期。 */
@Service
public class StockIndexDifferenceServiceImpl implements StockIndexDifferenceService {
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final IndexDailyQuoteMapper indexDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final StockIndexDifferenceCalculator stockIndexDifferenceCalculator;
    private final Clock clock;

    @Autowired
    public StockIndexDifferenceServiceImpl(StockDailyQuoteMapper stockDailyQuoteMapper,
            IndexDailyQuoteMapper indexDailyQuoteMapper, TradeCalendarService tradeCalendarService,
            StockIndexDifferenceCalculator stockIndexDifferenceCalculator) {
        this(stockDailyQuoteMapper, indexDailyQuoteMapper, tradeCalendarService,
                stockIndexDifferenceCalculator, Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    StockIndexDifferenceServiceImpl(StockDailyQuoteMapper stockDailyQuoteMapper,
            IndexDailyQuoteMapper indexDailyQuoteMapper, TradeCalendarService tradeCalendarService,
            StockIndexDifferenceCalculator stockIndexDifferenceCalculator, Clock clock) {
        this.stockDailyQuoteMapper = stockDailyQuoteMapper;
        this.indexDailyQuoteMapper = indexDailyQuoteMapper;
        this.tradeCalendarService = tradeCalendarService;
        this.stockIndexDifferenceCalculator = stockIndexDifferenceCalculator;
        this.clock = clock;
    }

    @Override
    public StockIndexDifferenceRespVo findByTradeDate(LocalDate tradeDate) {
        if (tradeDate == null) {
            throw new IllegalArgumentException("统计日期不能为空");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (tradeDate.isAfter(now.toLocalDate())) {
            throw new IllegalArgumentException("统计日期不能是未来日期");
        }
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalArgumentException("统计日期不是交易日");
        }
        if (tradeDate.equals(now.toLocalDate()) && now.toLocalTime().isBefore(LocalTime.of(15, 0))) {
            throw new IllegalStateException("当日尚未收盘，暂无法计算");
        }
        // 获取前一交易日
        LocalDate previousTradeDate = tradeCalendarService.getPreviousTradingDay(tradeDate, 1);
        List<LocalDate> dates = List.of(tradeDate, previousTradeDate);
        //根据日期和指数代码查询指数行情
        List<IndexDailyQuote> indices = indexDailyQuoteMapper.selectByTradeDatesAndCodes(
                dates, List.of(IndexStyleIndex.CSI_300.getIndexCode()));
        //从已经查出的沪深300行情列表中，取出指定日期的有效收盘价
        BigDecimal indexClose = closePrice(indices, tradeDate);
        BigDecimal indexPreviousClose = closePrice(indices, previousTradeDate);
        //返沪计算的指数与个股的差异值
        return stockIndexDifferenceCalculator.calculate(tradeDate, previousTradeDate,
                indexClose, indexPreviousClose, stockDailyQuoteMapper.selectClosePricesByTradeDates(dates));
    }

    /**
     * 从已经查出的指数行情列表中，取出指定日期的有效收盘价
     */
    private BigDecimal closePrice(List<IndexDailyQuote> indices, LocalDate date) {
        return indices.stream().filter(quote -> date.equals(quote.getTradeDate()))
                .map(IndexDailyQuote::getClosePrice).filter(price -> price != null && price.signum() > 0)
                .findFirst().orElseThrow(() -> new IllegalStateException("沪深300收盘行情缺失或无效：" + date));
    }
}
