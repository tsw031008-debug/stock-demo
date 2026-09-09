package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSourceService;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 指数分钟行情同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexMinuteQuoteSyncServiceImpl implements IndexMinuteQuoteSyncService {

    private static final LocalTime MORNING_START = LocalTime.of(9, 31);
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 1);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);
    private static final String SHANGHAI_COMPOSITE_CODE = "000001";
    private static final int COMPLETE_DAY_MINUTE_COUNT = 240;

    private final TradeCalendarService tradeCalendarService;
    private final IndexMinuteQuoteSourceService indexMinuteQuoteSourceService;
    private final IndexMinuteQuoteMapper indexMinuteQuoteMapper;

    /**
     * 同步指定分钟的上证指数行情。
     */
    @Override
    public int synchronize(LocalDateTime triggerTime) {
        Objects.requireNonNull(triggerTime, "任务触发时间不能为空");
        // 保留分钟 截断秒和纳秒
        LocalDateTime triggerMinute = triggerTime.truncatedTo(ChronoUnit.MINUTES);
        // 判断是否在采集时间
        if (!isCollectionTime(triggerMinute.toLocalTime())) {
            return 0;
        }
        // 判断是否是交易日
        if (!tradeCalendarService.isTradingDay(triggerTime.toLocalDate())) {
            log.debug("当天不是交易日，指数分钟行情同步跳过，tradeDate={}",
                    triggerTime.toLocalDate());
            return 0;
        }

        // 获取腾讯上证指数行情
        IndexMinuteQuote quote = indexMinuteQuoteSourceService.fetchShanghaiComposite();
        // 判断行情时间是否与任务时间一致
        LocalDateTime quoteMinute = quote.getQuoteTime().truncatedTo(ChronoUnit.MINUTES);
        if (!quoteMinute.equals(triggerMinute)) {
            throw new IllegalStateException("腾讯上证指数行情分钟与任务分钟不一致，expected="
                    + triggerMinute + "，actual=" + quoteMinute);
        }
        // 判断行情时间是否在采集时间
        if (!isCollectionTime(quoteMinute.toLocalTime())) {
            throw new IllegalStateException("腾讯上证指数行情时间不在采集区间，quoteTime="
                    + quoteMinute);
        }

        // 设置交易日
        quote.setTradeDate(quoteMinute.toLocalDate());
        quote.setQuoteTime(quoteMinute);
        return indexMinuteQuoteMapper.upsert(quote);
    }

    /**
     * 仅在数据库存在分钟缺口时请求腾讯当日完整分时，并补录真实缺失数据。
     */
    @Override
    public int recoverMissingMinutes(LocalDateTime checkTime) {
        Objects.requireNonNull(checkTime, "完整性检查时间不能为空");
        LocalDateTime actualCheckTime = checkTime.truncatedTo(ChronoUnit.MINUTES);
        if (!tradeCalendarService.isTradingDay(actualCheckTime.toLocalDate())) {
            return 0;
        }

        LocalTime endTime = resolveRecoveryEndTime(actualCheckTime.toLocalTime());
        if (endTime == null) {
            return 0;
        }
        List<LocalDateTime> expectedTimes = buildExpectedTimes(
                actualCheckTime.toLocalDate(),
                endTime
        );
        List<IndexMinuteQuote> existingQuotes = indexMinuteQuoteMapper
                .selectByIndexCodeAndQuoteTimeRange(
                        SHANGHAI_COMPOSITE_CODE,
                        expectedTimes.get(0),
                        expectedTimes.get(expectedTimes.size() - 1)
                );
        Set<LocalDateTime> existingTimes = new HashSet<>();
        for (IndexMinuteQuote quote : existingQuotes) {
            existingTimes.add(quote.getQuoteTime());
        }
        List<LocalDateTime> missingTimes = expectedTimes.stream()
                .filter(expectedTime -> !existingTimes.contains(expectedTime))
                .toList();
        if (missingTimes.isEmpty()) {
            return 0;
        }

        List<IndexMinuteQuote> sourceQuotes = indexMinuteQuoteSourceService
                .fetchShanghaiCompositeMinutes();
        Map<LocalDateTime, IndexMinuteQuote> sourceQuoteByTime = new HashMap<>();
        for (IndexMinuteQuote quote : sourceQuotes) {
            if (!SHANGHAI_COMPOSITE_CODE.equals(quote.getIndexCode())
                    || !actualCheckTime.toLocalDate().equals(quote.getTradeDate())) {
                throw new IllegalStateException("腾讯上证指数分时日期或代码不匹配，expectedDate="
                        + actualCheckTime.toLocalDate() + "，actualDate=" + quote.getTradeDate()
                        + "，actualCode=" + quote.getIndexCode());
            }
            if (sourceQuoteByTime.put(quote.getQuoteTime(), quote) != null) {
                throw new IllegalStateException("腾讯上证指数分时时间重复，quoteTime="
                        + quote.getQuoteTime());
            }
        }

        List<IndexMinuteQuote> missingQuotes = new ArrayList<>(missingTimes.size());
        for (LocalDateTime missingTime : missingTimes) {
            IndexMinuteQuote quote = sourceQuoteByTime.get(missingTime);
            if (quote == null) {
                throw new IllegalStateException("腾讯上证指数分时数据仍不完整，missingTime="
                        + missingTime);
            }
            missingQuotes.add(quote);
        }
        int savedCount = indexMinuteQuoteMapper.upsertBatch(missingQuotes);
        log.info("上证指数分钟缺口补录完成，tradeDate={}，endTime={}，missingCount={}，savedCount={}",
                actualCheckTime.toLocalDate(), endTime, missingQuotes.size(), savedCount);
        return savedCount;
    }

    /**
     * 盘前不检查；午休检查上午数据；收盘后检查全天240个时点。
     */
    private LocalTime resolveRecoveryEndTime(LocalTime checkTime) {
        if (checkTime.isBefore(MORNING_START)) {
            return null;
        }
        if (!checkTime.isAfter(MORNING_END) || checkTime.isBefore(AFTERNOON_START)) {
            return checkTime.isAfter(MORNING_END) ? MORNING_END : checkTime;
        }
        return checkTime.isAfter(AFTERNOON_END) ? AFTERNOON_END : checkTime;
    }

    private List<LocalDateTime> buildExpectedTimes(
            LocalDate tradeDate,
            LocalTime endTime
    ) {
        List<LocalDateTime> result = new ArrayList<>(COMPLETE_DAY_MINUTE_COUNT);
        appendMinuteRange(result, tradeDate, MORNING_START,
                endTime.isAfter(MORNING_END) ? MORNING_END : endTime);
        if (!endTime.isBefore(AFTERNOON_START)) {
            appendMinuteRange(result, tradeDate, AFTERNOON_START, endTime);
        }
        return result;
    }

    private void appendMinuteRange(
            List<LocalDateTime> result,
            LocalDate tradeDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        LocalDateTime current = LocalDateTime.of(tradeDate, startTime);
        LocalDateTime end = LocalDateTime.of(tradeDate, endTime);
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusMinutes(1);
        }
    }

    /**
     * 判断分钟是否属于240个有效采集时点。
     */
    private boolean isCollectionTime(LocalTime time) {
        boolean morning = !time.isBefore(MORNING_START) && !time.isAfter(MORNING_END);
        boolean afternoon = !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);
        return morning || afternoon;
    }
}
