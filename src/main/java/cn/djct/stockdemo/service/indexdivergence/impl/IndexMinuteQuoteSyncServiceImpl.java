package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSourceService;
import cn.djct.stockdemo.service.indexdivergence.IndexMinuteQuoteSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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
     * 判断分钟是否属于240个有效采集时点。
     */
    private boolean isCollectionTime(LocalTime time) {
        boolean morning = !time.isBefore(MORNING_START) && !time.isAfter(MORNING_END);
        boolean afternoon = !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);
        return morning || afternoon;
    }
}
