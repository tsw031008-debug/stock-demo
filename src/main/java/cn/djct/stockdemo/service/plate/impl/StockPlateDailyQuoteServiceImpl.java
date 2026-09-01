package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.common.StockPlateCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.StockPlateCloseDto;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.entity.StockPlateDailyQuote;
import cn.djct.stockdemo.service.plate.StockPlateDailyQuoteService;
import cn.djct.stockdemo.service.plate.StockPlateService;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 股票板块日线服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockPlateDailyQuoteServiceImpl implements StockPlateDailyQuoteService {

    private static final int SAVE_BATCH_SIZE = 500;

    private final TradeCalendarService tradeCalendarService;
    private final StockBasicService stockBasicService;
    private final StockDailyQuoteService stockDailyQuoteService;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StockPlateService stockPlateService;
    private final StockPlateDailyQuoteMapper stockPlateDailyQuoteMapper;
    private final StockPlateCalculator stockPlateCalculator;

    /**
     * 计算并保存指定交易日的完整板块日线。
     *
     * @param tradeDate 交易日
     * @return 保存的板块日线数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int synchronize(LocalDate tradeDate) {
        // 仅交易日生成板块日线，并确认当天股票日行情已经完整落库
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            return 0;
        }
        int expectedStockCount = stockBasicService.countSnapshot(tradeDate);
        int actualStockCount = stockDailyQuoteService.countByTradeDate(tradeDate);
        if (expectedStockCount == 0 || actualStockCount != expectedStockCount) {
            throw new IllegalStateException("当天股票日行情不完整，expected="
                    + expectedStockCount + "，actual=" + actualStockCount);
        }

        List<StockPlateMemberDto> members = requireMembers();
        int expectedPlateCount = distinctPlateCount(members);
        // 只有全部板块都是完整数据时才跳过，历史PARTIAL数据允许升级为COMPLETE
        if (stockPlateDailyQuoteMapper.countCompleteByTradeDate(tradeDate) == expectedPlateCount) {
            return 0;
        }

        // 使用交易日历确定年初首个交易日和上一交易日，不能按自然日推算
        List<LocalDate> tradingDays = tradeCalendarService.getTradingDays(
                LocalDate.of(tradeDate.getYear(), 1, 1),
                tradeDate
        );
        if (tradingDays.isEmpty()) {
            throw new IllegalStateException("当前年份没有交易日，tradeDate=" + tradeDate);
        }
        boolean firstTradingDay = tradingDays.get(0).equals(tradeDate);
        Map<Long, BigDecimal> previousClosePrices = firstTradingDay
                ? Map.of()
                : loadClosePrices(tradingDays.get(tradingDays.size() - 2));
        List<StockDailyQuote> stockQuotes = stockDailyQuoteMapper.selectForPlateCalculation(tradeDate);
        List<StockPlateDailyQuote> plateQuotes = stockPlateCalculator.calculateDailyQuotes(
                tradeDate,
                members,
                stockQuotes,
                previousClosePrices,
                firstTradingDay,
                "COMPLETE"
        );
        saveBatches(plateQuotes);
        validateDailyCount(tradeDate, plateQuotes.size());
        return plateQuotes.size();
    }

    /**
     * 使用当前成分股回补年初至结束日期的板块日线。
     *
     * @param endDate 结束交易日
     * @return 处理的板块日线数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int backfillCurrentYear(LocalDate endDate) {
        // 历史成分关系不可获得，因此整批回补统一标记为PARTIAL
        Objects.requireNonNull(endDate, "结束日期不能为空");
        if (!tradeCalendarService.isTradingDay(endDate)) {
            throw new IllegalArgumentException("结束日期必须是交易日");
        }
        List<LocalDate> tradingDays = tradeCalendarService.getTradingDays(
                LocalDate.of(endDate.getYear(), 1, 1),
                endDate
        );
        if (tradingDays.isEmpty()) {
            throw new IllegalStateException("当前年份没有交易日，endDate=" + endDate);
        }
        List<StockPlateMemberDto> members = requireMembers();
        Map<Long, BigDecimal> previousClosePrices = new HashMap<>();
        int processedCount = 0;
        for (int index = 0; index < tradingDays.size(); index++) {
            LocalDate tradeDate = tradingDays.get(index);
            List<StockDailyQuote> stockQuotes = stockDailyQuoteMapper.selectForPlateCalculation(tradeDate);
            if (stockQuotes.isEmpty()) {
                throw new IllegalStateException("缺少股票日行情，tradeDate=" + tradeDate);
            }
            List<StockPlateDailyQuote> plateQuotes = stockPlateCalculator.calculateDailyQuotes(
                    tradeDate,
                    members,
                    stockQuotes,
                    previousClosePrices,
                    index == 0,
                    "PARTIAL"
            );
            saveBatches(plateQuotes);
            previousClosePrices = toClosePriceMap(plateQuotes);
            processedCount += plateQuotes.size();
        }
        return processedCount;
    }

    /**
     * 读取非空的当前有效板块成分关系。
     */
    private List<StockPlateMemberDto> requireMembers() {
        List<StockPlateMemberDto> members = stockPlateService.findActiveMembers();
        if (members.isEmpty()) {
            throw new IllegalStateException("当前没有有效板块成分关系");
        }
        return members;
    }

    /**
     * 统计成分关系中的不同板块数量。
     */
    private int distinctPlateCount(List<StockPlateMemberDto> members) {
        return (int) members.stream()
                .map(StockPlateMemberDto::getPlateId)
                .distinct()
                .count();
    }

    /**
     * 查询指定交易日的板块收盘值并按板块ID建立索引。
     */
    private Map<Long, BigDecimal> loadClosePrices(LocalDate tradeDate) {
        Map<Long, BigDecimal> closePrices = new HashMap<>();
        for (StockPlateCloseDto close : stockPlateDailyQuoteMapper.selectCloseByTradeDate(tradeDate)) {
            closePrices.put(close.getPlateId(), close.getClosePrice());
        }
        return closePrices;
    }

    /**
     * 将本次板块日线转换为下一交易日需要的前收索引。
     */
    private Map<Long, BigDecimal> toClosePriceMap(List<StockPlateDailyQuote> quotes) {
        Map<Long, BigDecimal> closePrices = new HashMap<>();
        for (StockPlateDailyQuote quote : quotes) {
            closePrices.put(quote.getPlateId(), quote.getClosePrice());
        }
        return closePrices;
    }

    /**
     * 分批保存板块日线。
     */
    private void saveBatches(List<StockPlateDailyQuote> quotes) {
        if (quotes.isEmpty()) {
            throw new IllegalStateException("没有可保存的板块日线");
        }
        for (int startIndex = 0; startIndex < quotes.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, quotes.size());
            stockPlateDailyQuoteMapper.upsertBatch(
                    new ArrayList<>(quotes.subList(startIndex, endIndex))
            );
        }
    }

    /**
     * 校验指定交易日板块日线落库数量。
     */
    private void validateDailyCount(LocalDate tradeDate, int expectedCount) {
        int actualCount = stockPlateDailyQuoteMapper.countByTradeDate(tradeDate);
        if (actualCount != expectedCount) {
            throw new IllegalStateException("板块日线落库数量不一致，expected="
                    + expectedCount + "，actual=" + actualCount);
        }
    }
}
