package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketDailyTurnoverCalculator;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import cn.djct.stockdemo.service.marketlevel.MarketDailyTurnoverService;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

/**
 * 每日市场成交额同步服务实现。
 */
@Service
@RequiredArgsConstructor
public class MarketDailyTurnoverServiceImpl implements MarketDailyTurnoverService {

    private final MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StockBasicService stockBasicService;
    private final TradeCalendarService tradeCalendarService;
    private final MarketDailyTurnoverCalculator marketDailyTurnoverCalculator;
    private final StockDailyQuoteService stockDailyQuoteService;

    @Override
    public List<LocalDate> findMissingTradeDates(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "开始日期不能为空");
        Objects.requireNonNull(endDate, "结束日期不能为空");
        if (startDate.isAfter(endDate) || ChronoUnit.DAYS.between(startDate, endDate) > 30) {
            throw new IllegalArgumentException("缺口检查范围必须在31个自然日内");
        }
        Set<LocalDate> completeDates = marketDailyTurnoverMapper.selectCompleteByDateRange(startDate, endDate)
                .stream().map(MarketDailyTurnover::getTradeDate).collect(Collectors.toSet());
        return tradeCalendarService.getTradingDays(startDate, endDate).stream()
                .filter(date -> !completeDates.contains(date)).toList();
    }

    /**
     * 汇总并保存指定交易日的沪深A股成交额。
     *
     * @param tradeDate 交易日
     * @return 实际写入数量
     */
    @Override
    @Transactional
    public int synchronize(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日不能为空");
        // 非交易日不生成每日市场水位
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            return 0;
        }
        // 已有完整记录时直接跳过，保护正式汇总数据并保证任务幂等
        MarketDailyTurnover existing = marketDailyTurnoverMapper.selectByTradeDate(tradeDate);
        if (existing != null && "COMPLETE".equals(existing.getDataStatus())) {
            return 0;
        }

        // 下游汇总前确认当日股票清单与日行情数量一致，不能只依赖定时任务间隔判断数据就绪
        int expectedCount = stockBasicService.countSnapshot(tradeDate);
        int actualCount = stockDailyQuoteMapper.countByTradeDate(tradeDate);
        if (expectedCount <= 0 || actualCount != expectedCount) {
            throw new IllegalStateException("股票日行情尚未完整落库，tradeDate=" + tradeDate
                    + "，expected=" + expectedCount + "，actual=" + actualCount);
        }

        int checkedCount = 0;
        String lastCode = "";
        while (true) {
            List<StockBasic> stocks = stockBasicService.findSnapshotBatch(tradeDate, lastCode, 1000);
            if (stocks.isEmpty()) {
                break;
            }
            if (!stockDailyQuoteService.hasClosingQuotes(tradeDate,
                    stocks.stream().map(StockBasic::getStockCode).toList())) {
                throw new IllegalStateException("股票日行情含缺行、盘中或不完整记录，tradeDate=" + tradeDate);
            }
            checkedCount += stocks.size();
            lastCode = stocks.get(stocks.size() - 1).getStockCode();
        }
        if (checkedCount != expectedCount) {
            throw new IllegalStateException("股票快照覆盖校验失败，tradeDate=" + tradeDate);
        }

        // Mapper只读取原始成交额，逐股求和和完整性校验统一由计算组件完成
        MarketDailyTurnover turnover = marketDailyTurnoverCalculator.calculate(
                tradeDate,
                stockDailyQuoteMapper.selectMarketTurnoverRecords(List.of(tradeDate))
        );
        return marketDailyTurnoverMapper.upsert(turnover);
    }
}
