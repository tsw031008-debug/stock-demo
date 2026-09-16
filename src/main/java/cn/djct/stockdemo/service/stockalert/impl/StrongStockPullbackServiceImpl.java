package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.StrongStockPullbackCalculator;
import cn.djct.stockdemo.common.StockAlertCalculator;
import cn.djct.stockdemo.mapper.StockSelectionRunMapper;
import cn.djct.stockdemo.mapper.StockSelectionResultMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.service.stockalert.StrongStockPullbackService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import cn.djct.stockdemo.util.StockMarketCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 仅使用已落库日线，不请求外部行情。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrongStockPullbackServiceImpl implements StrongStockPullbackService {

    private static final String STRATEGY_TYPE = "STRONG_STOCK_PULLBACK";
    private static final int BATCH_SIZE = 100;

    private final TradeCalendarService tradeCalendarService;
    private final StockBasicMapper stockBasicMapper;
    private final StockSelectionRunMapper stockSelectionRunMapper;
    private final StockSelectionResultMapper stockSelectionResultMapper;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StrongStockPullbackCalculator strongStockPullbackCalculator;
    private final StockAlertCalculator stockAlertCalculator;

    @Override
    public boolean isCompleted(LocalDate tradeDate) {
        return stockSelectionRunMapper.isCompleted(tradeDate, STRATEGY_TYPE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int selectStocks(LocalDate tradeDate) {
        validateDate(tradeDate);
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            return 0;
        }
        // 预估当天股票基础快照数量
        int expectedCount = stockBasicMapper.countByLastSeenTradeDate(tradeDate);
        if (expectedCount == 0) {
            throw new IllegalStateException("当天股票基础快照尚未就绪");
        }
        // 获取61个交易日历日期
        LocalDate cutoff = tradeCalendarService.getPreviousTradingDay(tradeDate, 60);
        List<LocalDate> dates = tradeCalendarService.getTradingDays(cutoff, tradeDate);
        if (dates.size() != 61 || !dates.get(0).equals(cutoff) || !dates.get(60).equals(tradeDate)) {
            throw new IllegalStateException("强势股回调需要完整61个交易日历日期");
        }
        //获得最近50个交易日历日期
        List<LocalDate> calculationDates = dates.subList(11, 61);
        // 分批筛选，先收集全部符合条件的股票及当日换手率。
        List<StockBasic> selected = new ArrayList<>();
        Map<String, BigDecimal> turnoverRates = new HashMap<>();
        int processedCount = 0;
        int skippedCount = 0;
        int readyCount = 0;
        String lastCode = "";
        while (true) {
            // 批量获取股票基础信息
            List<StockBasic> stocks = stockBasicMapper.selectSnapshotAfterCode(tradeDate, lastCode, BATCH_SIZE);
            if (stocks.isEmpty()) {
                break;
            }
            // 根据股票代码和日期获取股票日线
            Map<String, Map<LocalDate, StockDailyQuote>> byStock = group(stockDailyQuoteMapper
                    .selectByStockCodesAndTradeDates(stocks.stream().map(StockBasic::getStockCode).toList(), calculationDates));
            for (StockBasic stock : stocks) {
                //获取对应的股票日线
                Map<LocalDate, StockDailyQuote> history = byStock.getOrDefault(stock.getStockCode(), Map.of());
                //获取当日股票日线
                StockDailyQuote today = history.get(tradeDate);
                //判断当日股票日线是否就绪
                if (today != null && "COMPLETE".equals(today.getDataStatus())) {
                    readyCount++;
                }
                String name = stock.getStockName();
                //去除ST
                if (name != null && (name.trim().startsWith("ST") || name.trim().startsWith("*ST"))) {
                    continue;
                }
                //停板
                if (today != null && today.getClosePrice() != null && today.getTurnoverAmountYuan() != null
                        && isSuspended(today)) {
                    continue;
                }
                //查找上市不足60日的
                String reason = invalidReason(stock, today, history, calculationDates, tradeDate);
                if (reason != null) {
                    skippedCount++;
                    log.warn("强势股回调跳过股票，stockCode={}，tradeDate={}，reason={}", stock.getStockCode(), tradeDate, reason);
                    continue;
                }
                //确认当前股票对应的涨停比例
                BigDecimal limitUpRate = StockMarketCodeUtil.currentNonStLimitUpRate(stock.getStockCode());
                //确认当前股票是否匹配强势股回调
                if (strongStockPullbackCalculator.matches(calculationDates.stream().map(history::get).toList(), limitUpRate)) {
                    selected.add(stock);
                    turnoverRates.put(stock.getStockCode(), today.getTurnoverRate());
                }
            }
            processedCount += stocks.size();
            lastCode = stocks.get(stocks.size() - 1).getStockCode();
        }
        // 全部当日日线未就绪时不能把尚未采集误报成成功零入选；个股缺失按确认口径跳过。
        if (readyCount == 0 || processedCount != expectedCount) {
            throw new IllegalStateException("当日日线尚未就绪或股票快照处理数量不一致");
        }
        // 全部筛选完成后统一排序，换手率相同按代码升序，最多取3只。
        selected.sort(Comparator.<StockBasic, BigDecimal>comparing(stock -> turnoverRates.get(stock.getStockCode()))
                .reversed().thenComparing(StockBasic::getStockCode));
        selected = selected.stream().limit(3).toList();
        stockSelectionResultMapper.deleteResults(tradeDate, STRATEGY_TYPE);
        if (!selected.isEmpty()) {
            stockSelectionResultMapper.insertResults(tradeDate, STRATEGY_TYPE, selected);
        }
        if (stockSelectionResultMapper.countResults(tradeDate, STRATEGY_TYPE) != selected.size()) {
            throw new IllegalStateException("强势股回调结果数量不一致");
        }
        stockSelectionRunMapper.completeRun(tradeDate, STRATEGY_TYPE);
        log.info("强势股回调计算完成，status={}，tradeDate={}，processedCount={}，selectedCount={}，skippedCount={}",
                skippedCount == 0 ? "SUCCESS" : "PARTIAL", tradeDate, processedCount, selected.size(), skippedCount);
        return selected.size();
    }


    // 筛选无效原因
    private String invalidReason(StockBasic stock, StockDailyQuote today,
                                 Map<LocalDate, StockDailyQuote> history, List<LocalDate> dates, LocalDate tradeDate) {
        if (stock.getStockName() == null || stock.getStockName().isBlank()) {
            return "当前股票名称缺失";
        }
        if (today == null || !"COMPLETE".equals(today.getDataStatus())
                || !positive(today.getOpenPrice()) || today.getChangePercent() == null
                || today.getTurnoverAmountYuan() == null || today.getTurnoverAmountYuan().signum() < 0
                || today.getTurnoverRate() == null || today.getTurnoverRate().signum() < 0) {
            return "当日日线未就绪或价格、涨幅、成交额、换手率无效";
        }
        // 按已有日线条数简化判断交易年龄，不能把未来记录计入。
        if (stockDailyQuoteMapper.countByStockCodeThroughTradeDate(stock.getStockCode(), tradeDate) <= 60) {
            return "截至计算日日线记录不足61条";
        }
        for (LocalDate date : dates) {
            StockDailyQuote quote = history.get(date);
            if (quote == null || !positive(quote.getPreviousClosePrice()) || !positive(quote.getClosePrice())
                    || !positive(quote.getHighPrice()) || !positive(quote.getLowPrice())
                    || quote.getHighPrice().compareTo(quote.getClosePrice()) < 0
                    || quote.getLowPrice().compareTo(quote.getClosePrice()) > 0) {
                return "历史日线缺失或价格无效：" + date;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageRespVo<LeftSideStockRespVo> findByTradeDate(LocalDate tradeDate, int pageNum, int pageSize) {
        validateDate(tradeDate);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("页码必须大于0，每页数量必须在1到100之间");
        }
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalArgumentException("交易日期不是交易日");
        }
        if (!stockSelectionRunMapper.isCompleted(tradeDate, STRATEGY_TYPE)) {
            throw new IllegalStateException("该交易日强势股回调尚无成功结果，任务未执行、执行中或执行失败");
        }
        long total = stockSelectionResultMapper.countResults(tradeDate, STRATEGY_TYPE);

        List<LeftSideStockRespVo> records = stockSelectionResultMapper.selectPageByTurnoverRate(tradeDate, STRATEGY_TYPE,
                (long) (pageNum - 1) * pageSize, pageSize);
        if (!records.isEmpty()) {
            List<LocalDate> baseDates = List.of(tradeCalendarService.getPreviousTradingDay(tradeDate, 3),
                    tradeCalendarService.getPreviousTradingDay(tradeDate, 5),
                    tradeCalendarService.getPreviousTradingDay(tradeDate, 10));
            Map<String, Map<LocalDate, StockDailyQuote>> history = group(stockDailyQuoteMapper.selectByStockCodesAndTradeDates(
                    records.stream().map(LeftSideStockRespVo::getStockCode).toList(), baseDates));
            for (LeftSideStockRespVo record : records) {
                if (!positive(record.getClosePrice()) || record.getOpenPrice() == null
                        || record.getHighPrice() == null || record.getLowPrice() == null
                        || record.getChangePercent() == null || record.getTurnoverAmountYuan() == null) {
                    throw new IllegalStateException("入选股票指定日期行情缺失：" + record.getStockCode());
                }
                Map<LocalDate, StockDailyQuote> stockHistory = history.getOrDefault(record.getStockCode(), Map.of());
                record.setThreeDayChangePercent(gain(record.getClosePrice(), stockHistory.get(baseDates.get(0))));
                record.setFiveDayChangePercent(gain(record.getClosePrice(), stockHistory.get(baseDates.get(1))));
                record.setTenDayChangePercent(gain(record.getClosePrice(), stockHistory.get(baseDates.get(2))));
            }
        }
        return PageRespVo.<LeftSideStockRespVo>builder().pageNum(pageNum).pageSize(pageSize)
                .total(total).records(records).build();
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("交易日期不能为空");
        }
        if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
            throw new IllegalArgumentException("交易日期不能是未来日期");
        }
    }

    private boolean isSuspended(StockDailyQuote quote) {
        return quote.getTurnoverAmountYuan().signum() == 0 && quote.getClosePrice().signum() == 0;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal gain(BigDecimal close, StockDailyQuote base) {
        if (base == null || !positive(base.getClosePrice())) {
            throw new IllegalStateException("入选股票涨幅计算所需历史收盘价缺失或无效");
        }
        return stockAlertCalculator.calculateGain(close, base.getClosePrice()).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Map<LocalDate, StockDailyQuote>> group(List<StockDailyQuote> quotes) {
        Map<String, Map<LocalDate, StockDailyQuote>> result = new HashMap<>();
        for (StockDailyQuote quote : quotes) {
            if (result.computeIfAbsent(quote.getStockCode(), key -> new HashMap<>())
                    .put(quote.getTradeDate(), quote) != null) {
                throw new IllegalStateException("股票日线存在重复日期：" + quote.getStockCode());
            }
        }
        return result;
    }
}
