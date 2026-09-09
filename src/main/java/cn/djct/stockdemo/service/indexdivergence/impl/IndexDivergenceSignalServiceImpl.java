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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<LocalDate, PreviousDayStatus> previousDayStatusCache =
            new ConcurrentHashMap<>();
    private final Set<LocalDate> blockedTradeDates = ConcurrentHashMap.newKeySet();

    /**
     * 前一完整交易日用于MACD预热，只保存当前分钟新确认的背离信号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateAndSave(LocalDateTime quoteTime) {
        Objects.requireNonNull(quoteTime, "行情时间不能为空");
        LocalDateTime currentMinute = quoteTime.truncatedTo(ChronoUnit.MINUTES);
        LocalDate tradeDate = quoteTime.toLocalDate();
        if (blockedTradeDates.contains(tradeDate)) {
            return 0;
        }

        PreviousDayStatus previousDayStatus = previousDayStatusCache.get(tradeDate);
        LocalDate previousTradeDate = previousDayStatus == null
                ? tradeCalendarService.getPreviousTradingDay(tradeDate, 1)
                : previousDayStatus.previousTradeDate();
        if (previousDayStatus != null && !previousDayStatus.complete()) {
            return 0;
        }
        LocalDateTime startTime = LocalDateTime.of(previousTradeDate, MORNING_START);
        // 获取前一交易日至当前分钟的指数分钟行情
        List<IndexMinuteQuote> quotes = indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                SHANGHAI_COMPOSITE_CODE,
                startTime,
                currentMinute
        );

        if (previousDayStatus == null) {
            List<IndexMinuteQuote> previousDayQuotes = quotes.stream()
                    .filter(item -> previousTradeDate.equals(item.getTradeDate()))
                    .toList();
            boolean complete = hasExpectedMinutes(
                    previousTradeDate,
                    previousDayQuotes,
                    AFTERNOON_END
            );
            previousDayStatusCache.put(
                    tradeDate,
                    new PreviousDayStatus(previousTradeDate, complete)
            );
            if (!complete) {
                log.info("前一交易日指数分钟行情不完整，当日后续跳过正式背离信号，"
                                + "如已补齐请重启应用，tradeDate={}，actualCount={}",
                        previousTradeDate, previousDayQuotes.size());
                return 0;
            }
        }

        List<IndexMinuteQuote> currentDayQuotes = quotes.stream()
                .filter(item -> tradeDate.equals(item.getTradeDate()))
                .toList();
        if (!hasExpectedMinutes(tradeDate, currentDayQuotes, currentMinute.toLocalTime())) {
            blockedTradeDates.add(tradeDate);
            log.warn("当前交易日指数分钟行情存在缺口，当日后续跳过正式背离信号，"
                            + "如已补齐请重启应用，tradeDate={}，currentMinute={}，actualCount={}",
                    tradeDate, currentMinute, currentDayQuotes.size());
            return 0;
        }

        // 计算MACD指标
        List<IndexMacdDto> macdItems = indexMacdCalculator.calculate(quotes);
        List<IndexDivergenceSignal> currentMinuteSignals = indexDivergenceSignalCalculator
                .detect(macdItems)
                .stream()
                .filter(signal -> currentMinute.equals(signal.getSignalTime()))
                .map(this::toEntity)
                .toList();
        if (currentMinuteSignals.isEmpty()) {
            return 0;
        }
        indexDivergenceSignalMapper.upsertBatch(currentMinuteSignals);
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

    private record PreviousDayStatus(LocalDate previousTradeDate, boolean complete) {
    }
}
