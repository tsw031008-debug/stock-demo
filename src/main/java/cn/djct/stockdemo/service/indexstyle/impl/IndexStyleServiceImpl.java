package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexStyleCalculator;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSourceService;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 指数大小风格查询服务实现。
 */
@Service
public class IndexStyleServiceImpl implements IndexStyleService {

    private static final int TRADING_DAY_COUNT = 5;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final IndexDailyQuoteMapper indexDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final IndexStyleCalculator indexStyleCalculator;
    private final IndexDailyQuoteSourceService sourceService;
    private final Clock clock;

    /**
     * 创建指数大小风格查询服务。
     */
    @Autowired
    public IndexStyleServiceImpl(
            IndexDailyQuoteMapper indexDailyQuoteMapper,
            TradeCalendarService tradeCalendarService,
            IndexStyleCalculator indexStyleCalculator,
            IndexDailyQuoteSourceService sourceService
    ) {
        this(
                indexDailyQuoteMapper,
                tradeCalendarService,
                indexStyleCalculator,
                sourceService,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的查询服务，供交易时间边界测试使用。
     */
    IndexStyleServiceImpl(
            IndexDailyQuoteMapper indexDailyQuoteMapper,
            TradeCalendarService tradeCalendarService,
            IndexStyleCalculator indexStyleCalculator,
            IndexDailyQuoteSourceService sourceService,
            Clock clock
    ) {
        this.indexDailyQuoteMapper = indexDailyQuoteMapper;
        this.tradeCalendarService = tradeCalendarService;
        this.indexStyleCalculator = indexStyleCalculator;
        this.sourceService = sourceService;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 交易日开盘后包含今日实时行情，比较包含统计日在内的最近五个交易日。
     */
    @Override
    public IndexStyleComparisonDto getLatest() {
        LocalDateTime currentDateTime = LocalDateTime.now(clock);
        LocalDate currentDate = currentDateTime.toLocalDate();
        LocalTime currentTime = currentDateTime.toLocalTime();
        LocalDate latestStoredDate = indexDailyQuoteMapper.selectLatestTradeDate();
        boolean useCurrentQuote = tradeCalendarService.isTradingDay(currentDate)
                && !currentTime.isBefore(MARKET_OPEN_TIME)
                && !currentDate.equals(latestStoredDate);
        LocalDate statisticsDate = useCurrentQuote ? currentDate : latestStoredDate;
        if (statisticsDate == null) {
            throw new IllegalStateException("没有可用的指数日行情数据");
        }
        // 五日窗口包含统计日，所以开始日是统计日前第4个交易日。
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
        List<LocalDate> storedTradeDates = useCurrentQuote
                ? tradeDates.subList(0, tradeDates.size() - 1)
                : tradeDates;
        List<IndexDailyQuote> quotes = new ArrayList<>(
                indexDailyQuoteMapper.selectByTradeDatesAndCodes(
                        storedTradeDates,
                        indexCodes
                )
        );
        if (useCurrentQuote) {
            // 今日实时指数行情只参与本次强弱计算，不作为最终收盘行情入库。
            quotes.addAll(sourceService.fetch(currentDate));
        }
        return IndexStyleComparisonDto.builder()
                .statisticsDate(statisticsDate)
                .tradeDates(tradeDates)
                .indices(indexStyleCalculator.calculate(tradeDates, quotes))
                .build();
    }
}
