package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.common.IndexDivergenceSignalCalculator;
import cn.djct.stockdemo.common.IndexMacdCalculator;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexDivergenceSignalDto;
import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceSignalService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 指数MACD背离信号服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexDivergenceSignalServiceImpl implements IndexDivergenceSignalService {

    private static final String SHANGHAI_COMPOSITE_CODE = "000001";
    private static final int COMPLETE_DAY_MINUTE_COUNT = 240;
    private static final LocalTime MORNING_START = LocalTime.of(9, 31);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 1);

    private final TradeCalendarService tradeCalendarService;
    private final IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    private final IndexDivergenceSignalMapper indexDivergenceSignalMapper;
    private final IndexMacdCalculator indexMacdCalculator;
    private final IndexDivergenceSignalCalculator indexDivergenceSignalCalculator;

    /**
     * 前一完整交易日用于MACD预热，当前交易日只保存已经结束的背离信号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateAndSave(LocalDateTime quoteTime) {
        Objects.requireNonNull(quoteTime, "行情时间不能为空");
        LocalDateTime currentMinute = quoteTime.truncatedTo(ChronoUnit.MINUTES);
        LocalDate tradeDate = currentMinute.toLocalDate();
        // 获取前一交易日
        LocalDate previousTradeDate = tradeCalendarService.getPreviousTradingDay(tradeDate, 1);
        LocalDateTime startTime = LocalDateTime.of(previousTradeDate, MORNING_START);
        // 获取当前交易日的指数分钟行情
        List<IndexMinuteQuote> quotes = indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                SHANGHAI_COMPOSITE_CODE,
                startTime,
                currentMinute
        );
        // 过滤出前一交易日的指数分钟行情数据
        List<IndexMinuteQuote> previousDayQuotes = quotes.stream()
                .filter(item -> previousTradeDate.equals(item.getTradeDate()))
                .toList();
        // 校验前一交易日的指数分钟行情是否完整
        if (!isCompleteTradingDay(previousTradeDate, previousDayQuotes)) {
            log.info("前一交易日指数分钟行情不完整，跳过正式背离信号，tradeDate={}，actualCount={}",
                    previousTradeDate, previousDayQuotes.size());
            return 0;
        }

        // 计算MACD指标
        List<IndexMacdDto> macdItems = indexMacdCalculator.calculate(quotes);
        List<IndexDivergenceSignal> currentDaySignals = indexDivergenceSignalCalculator
                .detect(macdItems)
                .stream()
                .filter(signal -> tradeDate.equals(signal.getSignalTime().toLocalDate()))
                .map(this::toEntity)
                .toList();
        if (currentDaySignals.isEmpty()) {
            return 0;
        }
        indexDivergenceSignalMapper.upsertBatch(currentDaySignals);
        return currentDaySignals.size();
    }

    /**
     * 校验240个分钟时点，避免仅凭数量掩盖分钟缺口或午休脏数据。
     */
    private boolean isCompleteTradingDay(LocalDate tradeDate, List<IndexMinuteQuote> quotes) {
        if (quotes.size() != COMPLETE_DAY_MINUTE_COUNT) {
            return false;
        }
        List<LocalDateTime> expectedTimes = new ArrayList<>(COMPLETE_DAY_MINUTE_COUNT);
        LocalDateTime morning = LocalDateTime.of(tradeDate, MORNING_START);
        for (int index = 0; index < 120; index++) {
            expectedTimes.add(morning.plusMinutes(index));
        }
        LocalDateTime afternoon = LocalDateTime.of(tradeDate, AFTERNOON_START);
        for (int index = 0; index < 120; index++) {
            expectedTimes.add(afternoon.plusMinutes(index));
        }
        for (int index = 0; index < COMPLETE_DAY_MINUTE_COUNT; index++) {
            if (!expectedTimes.get(index).equals(quotes.get(index).getQuoteTime())) {
                return false;
            }
        }
        return true;
    }

    private IndexDivergenceSignal toEntity(IndexDivergenceSignalDto signal) {
        return IndexDivergenceSignal.builder()
                .indexCode(SHANGHAI_COMPOSITE_CODE)
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
