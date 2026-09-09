package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.vo.IndexDivergenceLatestRespVo;
import cn.djct.stockdemo.pojo.vo.IndexDivergenceSignalRespVo;
import cn.djct.stockdemo.pojo.vo.IndexMinuteCurvePointRespVo;
import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceQueryService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 指数曲线和背离信号查询服务实现。
 */
@Service
public class IndexDivergenceQueryServiceImpl implements IndexDivergenceQueryService {

    private static final String SHANGHAI_COMPOSITE_CODE = "000001";
    private static final LocalTime FIRST_MINUTE = LocalTime.of(9, 31);
    private static final LocalTime LAST_MINUTE = LocalTime.of(15, 0);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final TradeCalendarService tradeCalendarService;
    private final IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    private final IndexDivergenceSignalMapper indexDivergenceSignalMapper;
    private final Clock clock;

    @Autowired
    public IndexDivergenceQueryServiceImpl(
            TradeCalendarService tradeCalendarService,
            IndexMinuteQuoteMapper indexMinuteQuoteMapper,
            IndexDivergenceSignalMapper indexDivergenceSignalMapper
    ) {
        this(
                tradeCalendarService,
                indexMinuteQuoteMapper,
                indexDivergenceSignalMapper,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    IndexDivergenceQueryServiceImpl(
            TradeCalendarService tradeCalendarService,
            IndexMinuteQuoteMapper indexMinuteQuoteMapper,
            IndexDivergenceSignalMapper indexDivergenceSignalMapper,
            Clock clock
    ) {
        this.tradeCalendarService = tradeCalendarService;
        this.indexMinuteQuoteMapper = indexMinuteQuoteMapper;
        this.indexDivergenceSignalMapper = indexDivergenceSignalMapper;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 交易日09:31后返回当天数据，否则返回上一交易日数据。
     */
    @Override
    public IndexDivergenceLatestRespVo getLatest() {
        // 获取当前日期和时间
        LocalDate currentDate = LocalDate.now(clock);
        LocalTime currentTime = LocalTime.now(clock);
        // 确定查询日期 9:31之前返回上一交易日日期
        LocalDate tradeDate = resolveTradeDate(currentDate, currentTime);
        // 查询上证指数分钟K线数据
        List<IndexMinuteQuote> quotes = indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                SHANGHAI_COMPOSITE_CODE,
                LocalDateTime.of(tradeDate, FIRST_MINUTE),
                LocalDateTime.of(tradeDate, LAST_MINUTE)
        );
        // 查询上证指数背离信号数据
        List<IndexDivergenceSignal> signals =
                indexDivergenceSignalMapper.selectByIndexCodeAndSignalTimeRange(
                        SHANGHAI_COMPOSITE_CODE,
                        tradeDate.atStartOfDay(),
                        tradeDate.plusDays(1).atStartOfDay()
                );
        return IndexDivergenceLatestRespVo.builder()
                .tradeDate(tradeDate)
                .previousClosePrice(quotes.isEmpty() ? null : quotes.get(0).getPreviousClosePrice())
                .curveData(quotes.stream().map(this::toCurvePoint).toList())
                .signalData(signals.stream().map(this::toSignal).toList())
                .build();
    }

    private LocalDate resolveTradeDate(LocalDate currentDate, LocalTime currentTime) {
        if (tradeCalendarService.isTradingDay(currentDate)
                && !currentTime.isBefore(FIRST_MINUTE)) {
            return currentDate;
        }
        // 获取上一个交易日
        return tradeCalendarService.getPreviousTradingDay(currentDate, 1);
    }

    private IndexMinuteCurvePointRespVo toCurvePoint(IndexMinuteQuote quote) {
        return IndexMinuteCurvePointRespVo.builder()
                .quoteTime(quote.getQuoteTime())
                .currentPrice(quote.getCurrentPrice())
                .build();
    }

    private IndexDivergenceSignalRespVo toSignal(IndexDivergenceSignal signal) {
        return IndexDivergenceSignalRespVo.builder()
                .signalType(signal.getSignalType())
                .signalTime(signal.getSignalTime())
                .previousIntervalStartTime(signal.getPreviousIntervalStartTime())
                .previousIntervalEndTime(signal.getPreviousIntervalEndTime())
                .currentIntervalStartTime(signal.getCurrentIntervalStartTime())
                .currentIntervalEndTime(signal.getCurrentIntervalEndTime())
                .previousPriceExtreme(signal.getPreviousPriceExtreme())
                .currentPriceExtreme(signal.getCurrentPriceExtreme())
                .previousMacdExtreme(signal.getPreviousMacdExtreme())
                .currentMacdExtreme(signal.getCurrentMacdExtreme())
                .previousDifExtreme(signal.getPreviousDifExtreme())
                .currentDifExtreme(signal.getCurrentDifExtreme())
                .build();
    }
}
