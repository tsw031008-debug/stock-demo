package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.common.IndexDivergenceSignalCalculator;
import cn.djct.stockdemo.common.IndexMacdCalculator;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.vo.IndexDivergenceSignalRespVo;
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
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 1);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    private final TradeCalendarService tradeCalendarService;
    private final IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    private final IndexDivergenceSignalMapper indexDivergenceSignalMapper;
    private final IndexMacdCalculator indexMacdCalculator;
    private final IndexDivergenceSignalCalculator indexDivergenceSignalCalculator;
    /**
     * 前一完整交易日用于MACD预热，只保存当前分钟新确认的背离信号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateAndSave(LocalDateTime quoteTime) {
        Objects.requireNonNull(quoteTime, "行情时间不能为空");
        return calculate(quoteTime.truncatedTo(ChronoUnit.MINUTES), false);
    }

    /** 补录后重算当日已确认信号，前一交易日仍仅用于预热。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recoverAndSave(LocalDateTime checkTime) {
        Objects.requireNonNull(checkTime, "检查时间不能为空");
        LocalTime endTime = checkTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        if (endTime.isBefore(MORNING_START)
                || !tradeCalendarService.isTradingDay(checkTime.toLocalDate())) {
            return 0;
        }
        if (endTime.isAfter(AFTERNOON_END)) {
            endTime = AFTERNOON_END;
        } else if (endTime.isAfter(MORNING_END) && endTime.isBefore(AFTERNOON_START)) {
            endTime = MORNING_END;
        }
        return calculate(checkTime.toLocalDate().atTime(endTime), true);
    }

    private int calculate(LocalDateTime currentMinute, boolean recovery) {
        if (!isCollectionTime(currentMinute.toLocalTime())) {
            return 0;
        }
        LocalDate tradeDate = currentMinute.toLocalDate();
        LocalDate previousTradeDate = tradeCalendarService.getPreviousTradingDay(tradeDate, 1);
        List<IndexMinuteQuote> quotes = indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                SHANGHAI_COMPOSITE_CODE, previousTradeDate.atTime(MORNING_START), currentMinute);
        // 不缓存失败状态：补齐行情后，下次计算必须能够重新验证并恢复。
        List<IndexMinuteQuote> previousDayQuotes = quotes.stream()
                .filter(item -> previousTradeDate.equals(item.getTradeDate())).toList();
        List<IndexMinuteQuote> currentDayQuotes = quotes.stream()
                .filter(item -> tradeDate.equals(item.getTradeDate())).toList();
        if (!hasExpectedMinutes(previousTradeDate, previousDayQuotes, AFTERNOON_END)
                || !hasExpectedMinutes(tradeDate, currentDayQuotes, currentMinute.toLocalTime())) {
            log.warn("指数分钟行情不完整，跳过本次信号计算，tradeDate={}，currentMinute={}，recovery={}",
                    tradeDate, currentMinute, recovery);
            return 0;
        }

        // 计算MACD指标
        List<IndexMacdDto> macdItems = indexMacdCalculator.calculate(quotes);
        List<IndexDivergenceSignal> currentMinuteSignals = indexDivergenceSignalCalculator
                .detect(macdItems)
                .stream()
                .filter(signal -> recovery
                        ? tradeDate.equals(signal.getSignalTime().toLocalDate())
                            && !signal.getSignalTime().isAfter(currentMinute)
                        : currentMinute.equals(signal.getSignalTime()))
                .map(this::toEntity)
                .toList();
        if (currentMinuteSignals.isEmpty()) {
            return 0;
        }
        indexDivergenceSignalMapper.upsertBatch(currentMinuteSignals);
        log.info("指数背离信号保存完成，tradeDate={}，endTime={}，recovery={}，signalCount={}",
                tradeDate, currentMinute, recovery, currentMinuteSignals.size());
        return currentMinuteSignals.size();
    }

    /**
     * 校验指定结束分钟之前的全部预期时点。
     */
    private boolean hasExpectedMinutes(
            LocalDate tradeDate,
            List<IndexMinuteQuote> quotes,
            LocalTime endTime
    ) {
        List<LocalDateTime> expectedTimes = buildExpectedTimes(tradeDate, endTime);
        if (expectedTimes.isEmpty() || quotes.size() != expectedTimes.size()) {
            return false;
        }
        for (int index = 0; index < expectedTimes.size(); index++) {
            if (!expectedTimes.get(index).equals(quotes.get(index).getQuoteTime())) {
                return false;
            }
        }
        return true;
    }

    private List<LocalDateTime> buildExpectedTimes(LocalDate tradeDate, LocalTime endTime) {
        if (!isCollectionTime(endTime)) {
            return List.of();
        }
        List<LocalDateTime> expectedTimes = new ArrayList<>(COMPLETE_DAY_MINUTE_COUNT);
        LocalTime morningLimit = endTime.isBefore(MORNING_END) ? endTime : MORNING_END;
        appendMinuteRange(expectedTimes, tradeDate, MORNING_START, morningLimit);
        if (!endTime.isBefore(AFTERNOON_START)) {
            appendMinuteRange(expectedTimes, tradeDate, AFTERNOON_START, endTime);
        }
        return expectedTimes;
    }

    private void appendMinuteRange(
            List<LocalDateTime> expectedTimes,
            LocalDate tradeDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        LocalDateTime current = LocalDateTime.of(tradeDate, startTime);
        LocalDateTime end = LocalDateTime.of(tradeDate, endTime);
        while (!current.isAfter(end)) {
            expectedTimes.add(current);
            current = current.plusMinutes(1);
        }
    }

    private boolean isCollectionTime(LocalTime time) {
        boolean morning = !time.isBefore(MORNING_START) && !time.isAfter(MORNING_END);
        boolean afternoon = !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);
        return morning || afternoon;
    }

    private IndexDivergenceSignal toEntity(IndexDivergenceSignalRespVo signal) {
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
