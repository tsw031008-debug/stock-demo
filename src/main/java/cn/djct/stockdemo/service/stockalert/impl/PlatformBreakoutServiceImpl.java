package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.PlatformBreakoutCalculator;
import cn.djct.stockdemo.common.StockAlertCalculator;
import cn.djct.stockdemo.mapper.StockSelectionRunMapper;
import cn.djct.stockdemo.mapper.StockSelectionResultMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.service.stockalert.PlatformBreakoutService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 仅使用已落库日线，不请求外部行情。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformBreakoutServiceImpl implements PlatformBreakoutService {

    private static final String STRATEGY_TYPE = "PLATFORM_BREAKOUT";
    private static final int BATCH_SIZE = 100;

    private final TradeCalendarService tradeCalendarService;
    private final StockBasicMapper stockBasicMapper;
    private final StockSelectionRunMapper stockSelectionRunMapper;
    private final StockSelectionResultMapper stockSelectionResultMapper;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final PlatformBreakoutCalculator platformBreakoutCalculator;
    private final StockAlertCalculator stockAlertCalculator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int selectStocks(LocalDate tradeDate) {
        validateDate(tradeDate);
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            return 0;
        }
        //查当天股票基础快照数量
        int expectedCount = stockBasicMapper.countByLastSeenTradeDate(tradeDate);
        if (expectedCount == 0) {
            throw new IllegalStateException("当天股票基础快照尚未就绪");
        }
        //查近200个交易日历日期，按照T-199到T升序排列
        LocalDate startDate = tradeCalendarService.getPreviousTradingDay(tradeDate, 199);
        List<LocalDate> dates = tradeCalendarService.getTradingDays(startDate, tradeDate);
        if (dates.size() != 200 || !dates.get(0).equals(startDate)//避免日期错误
                || !dates.get(199).equals(tradeDate)) {
            throw new IllegalStateException("平台突破需要完整200个交易日历日期");
        }
        //清除当天平台突破结果，重新计算
        stockSelectionResultMapper.deleteResults(tradeDate, STRATEGY_TYPE);
        int processedCount = 0;
        int selectedCount = 0;
        int insufficientCount = 0;
        int invalidHistoryCount = 0;
        String lastCode = "";
        while (true) {
            //根据交易日期、股票代码和批次大小，分批获取股票快照。
            List<StockBasic> stocks = stockBasicMapper.selectSnapshotAfterCode(tradeDate, lastCode, BATCH_SIZE);
            if (stocks.isEmpty()) {
                break;
            }
            //获取股票代码
            List<String> codes = stocks.stream().map(StockBasic::getStockCode).toList();
            //根据股票代码和交易日期范围，分批获取股票行情。
            Map<String, Map<LocalDate, StockDailyQuote>> byStock = group(stockDailyQuoteMapper.selectByStockCodesAndTradeDates(codes, dates));
            List<StockBasic> selected = new ArrayList<>();
            for (StockBasic stock : stocks) {
                //根据股票代码获取历史行情
                Map<LocalDate, StockDailyQuote> history = byStock.getOrDefault(stock.getStockCode(), Map.of());
                //根据今天日期获取行情
                StockDailyQuote today = history.get(tradeDate);
                //过滤无效数据：停牌（成交额和当前价为0）或无效数据
                requireToday(today, stock.getStockCode());
                String name = stock.getStockName();
                //平台突破开头的过滤
                if (name == null || name.isBlank()) {
                    throw new IllegalStateException("股票名称缺失：" + stock.getStockCode());
                }
                if (name.trim().startsWith("ST") || name.trim().startsWith("*ST")
                        || isSuspended(today)) {
                    continue;
                }
                List<StockDailyQuote> ordered = new ArrayList<>();
                // 从dates中找到第一个包含在history中的日期firstAvailable，作为历史行情的起始日期
                LocalDate firstAvailable = dates.stream().filter(history::containsKey).findFirst().orElseThrow();
                boolean invalidHistory = false;
                for (LocalDate date : dates) {
                    if (date.isBefore(firstAvailable)) {
                        continue;
                    }
                    //从firstAvailable开始获取对应的行情数据
                    StockDailyQuote quote = history.get(date);
                    // 历史缺口或无效价格只影响当前股票，不补0、不缩短窗口计算。
                    if (quote == null || !positive(quote.getClosePrice()) || !positive(quote.getHighPrice())
                            || quote.getHighPrice().compareTo(quote.getClosePrice()) < 0) {
                        log.warn("平台突破跳过历史行情异常股票，stockCode={}，tradeDate={}，missingOrInvalidDate={}",
                                stock.getStockCode(), tradeDate, date);
                        invalidHistory = true;
                        break;
                    }
                    //按日期顺序添加行情
                    ordered.add(quote);
                }
                if (invalidHistory) {
                    invalidHistoryCount++;
                    continue;
                }
                //避免由于前面过滤或者历史行情异常导致的不足200个交易日，记录并跳过
                if (ordered.size() < 200) {
                    if (!firstAvailable.equals(stockDailyQuoteMapper.selectFirstQuoteDate(stock.getStockCode()))) {
                        invalidHistoryCount++;
                        log.warn("平台突破跳过历史窗口前端缺失股票，stockCode={}，tradeDate={}，firstAvailable={}",
                                stock.getStockCode(), tradeDate, firstAvailable);
                        continue;
                    }
                    //统计历史不足200个交易日的股票数量
                    insufficientCount++;
                    log.info("平台突破历史不足，跳过，stockCode={}，availableDays={}", stock.getStockCode(), ordered.size());
                    continue;
                }
                //进行平台突破匹配
                if (platformBreakoutCalculator.matches(ordered)) {
                    selected.add(stock);
                }
            }
            if (!selected.isEmpty()) {
                stockSelectionResultMapper.insertResults(tradeDate, STRATEGY_TYPE, selected);
            }
            selectedCount += selected.size();
            processedCount += stocks.size();
            lastCode = stocks.get(stocks.size() - 1).getStockCode();
        }
        //验证处理数量和结果数量一致
        if (processedCount != expectedCount || stockSelectionResultMapper.countResults(tradeDate, STRATEGY_TYPE) != selectedCount) {
            throw new IllegalStateException("平台突破股票处理数量或结果数量不一致");
        }
        //标记任务完成
        stockSelectionRunMapper.completeRun(tradeDate, STRATEGY_TYPE);
        log.info("平台突破计算完成，status={}，tradeDate={}，processedCount={}，selectedCount={}，insufficientCount={}，invalidHistoryCount={}",
                invalidHistoryCount == 0 ? "SUCCESS" : "PARTIAL", tradeDate, processedCount,
                selectedCount, insufficientCount, invalidHistoryCount);
        return selectedCount;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageRespVo<LeftSideStockRespVo> findByTradeDate(LocalDate tradeDate, int pageNum, int pageSize) {
        validateDate(tradeDate);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("页码必须大于0，每页数量必须在1到100之间");
        }
        //判断交易日
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalArgumentException("交易日期不是交易日");
        }
        //判断该任务是否完成
        if (!stockSelectionRunMapper.isCompleted(tradeDate, STRATEGY_TYPE)) {
            throw new IllegalStateException("该交易日平台突破尚无成功结果，任务未执行、执行中或执行失败");
        }
        //获取该任务完成后记录的总数量
        long total = stockSelectionResultMapper.countResults(tradeDate, STRATEGY_TYPE);

        //分页查询任务记录数
        List<LeftSideStockRespVo> records = stockSelectionResultMapper.selectPage(tradeDate, STRATEGY_TYPE,
                (long) (pageNum - 1) * pageSize, pageSize);
        if (!records.isEmpty()) {
            List<LocalDate> baseDates = List.of(tradeCalendarService.getPreviousTradingDay(tradeDate, 3),
                    tradeCalendarService.getPreviousTradingDay(tradeDate, 5),
                    tradeCalendarService.getPreviousTradingDay(tradeDate, 10));
            Map<String, Map<LocalDate, StockDailyQuote>> history = group(stockDailyQuoteMapper.selectByStockCodesAndTradeDates(
                    records.stream().map(LeftSideStockRespVo::getStockCode).toList(), baseDates));
            for (LeftSideStockRespVo record : records) {
                //参数校验
                if (!positive(record.getClosePrice()) || record.getOpenPrice() == null
                        || record.getHighPrice() == null || record.getLowPrice() == null
                        || record.getChangePercent() == null || record.getTurnoverAmountYuan() == null) {
                    throw new IllegalStateException("入选股票指定日期行情缺失：" + record.getStockCode());
                }
                //获取指定股票在指定交易日的行情，没有则使用空Map
                Map<LocalDate, StockDailyQuote> stockHistory = history.getOrDefault(record.getStockCode(), Map.of());
                //计算3日涨幅
                record.setThreeDayChangePercent(gain(record.getClosePrice(), stockHistory.get(baseDates.get(0))));
                //计算5日涨幅
                record.setFiveDayChangePercent(gain(record.getClosePrice(), stockHistory.get(baseDates.get(1))));
                //计算10日涨幅
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

    private void requireToday(StockDailyQuote quote, String code) {
        if (quote == null || !"COMPLETE".equals(quote.getDataStatus())
                || quote.getTurnoverAmountYuan() == null || quote.getTurnoverAmountYuan().signum() < 0
                || quote.getClosePrice() == null) {
            throw new IllegalStateException("当日日线尚未完整落库：" + code);
        }

        //判断是否停牌
        if (!isSuspended(quote) && (!positive(quote.getClosePrice()) || !positive(quote.getOpenPrice())
                || !positive(quote.getHighPrice()) || !positive(quote.getLowPrice())
                || quote.getHighPrice().compareTo(quote.getClosePrice()) < 0
                || quote.getLowPrice().compareTo(quote.getClosePrice()) > 0 || quote.getChangePercent() == null)) {
            throw new IllegalStateException("当日日线价格或涨幅无效：" + code);
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
