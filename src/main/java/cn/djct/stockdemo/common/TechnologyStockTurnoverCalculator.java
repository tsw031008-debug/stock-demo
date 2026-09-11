package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankRespVo;
import cn.djct.stockdemo.pojo.vo.TechnologyStockTurnoverRespVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 科技股成交额异动：金额单位为元，所有阈值均为严格大于。 */
@Component
public class TechnologyStockTurnoverCalculator {
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");
    private static final BigDecimal FIVE_HUNDRED_MILLION = new BigDecimal("500000000");
    private static final BigDecimal GROWTH_MULTIPLIER = new BigDecimal("1.05");
    private static final BigDecimal ABNORMAL_MULTIPLIER = new BigDecimal("2");
    private static final int RANKING_LIMIT = 5;

    /** dates按T、T-1、T-2、T-3排列，三类独立；历史缺失只影响依赖它的分类。 */
    public TechnologyStockTurnoverRespVo calculate(List<LocalDate> dates, List<StockDailyQuote> quotes) {
        if (dates == null || dates.size() != 4 || dates.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("必须提供T至T-3四个交易日");
        }

        if (quotes == null) {
            throw new IllegalStateException("股票日行情查询结果为空");
        }
        Map<String, Map<LocalDate, StockDailyQuote>> byStock = new HashMap<>();
        //按照股票代码分组
        for (StockDailyQuote quote : quotes) {
            if (quote == null || quote.getStockCode() == null || quote.getStockCode().isBlank()
                    || quote.getTradeDate() == null || !dates.contains(quote.getTradeDate())) {
                throw new IllegalStateException("股票日行情代码或日期无效");
            }
            //检查并添加股票日行情
            if (byStock.computeIfAbsent(quote.getStockCode(), key -> new HashMap<>())
                    .putIfAbsent(quote.getTradeDate(), quote) != null) {
                throw new IllegalStateException("存在重复股票日行情");
            }
        }
        List<StockDailyQuote> increasing = new ArrayList<>();
        List<StockDailyQuote> institutional = new ArrayList<>();
        List<StockDailyQuote> abnormal = new ArrayList<>();
        //遍历每只股票的行情
        for (Map<LocalDate, StockDailyQuote> daily : byStock.values()) {
            StockDailyQuote current = daily.get(dates.get(0));
            //去除无效数据 涨幅大于0，成交额大于1亿元
            if (current == null || current.getStockName() == null || current.getStockName().isBlank()
                    || current.getStockName().contains("ST")
                    || current.getChangePercent() == null || current.getChangePercent().signum() <= 0
                    || current.getTurnoverAmountYuan() == null
                    || current.getTurnoverAmountYuan().compareTo(ONE_HUNDRED_MILLION) <= 0) {
                continue;
            }
            //获取前一日行情
            StockDailyQuote previous = daily.get(dates.get(1));
            //判断是否为统计日和前一日的涨幅是否超过5%
            if (exceeds(current, previous, GROWTH_MULTIPLIER)) {
                if (exceeds(previous, daily.get(dates.get(2)), GROWTH_MULTIPLIER)
                        && exceeds(daily.get(dates.get(2)), daily.get(dates.get(3)), GROWTH_MULTIPLIER)) {
                    increasing.add(current);
                }
                //判断是否为机构股：即统计日的成交额是否超过50000万元
                if (current.getTurnoverAmountYuan().compareTo(FIVE_HUNDRED_MILLION) > 0) {
                    institutional.add(current);
                }
            }
            //判断是否为异常股：即统计日的成交额是否超过前一日的2倍
            if (exceeds(current, previous, ABNORMAL_MULTIPLIER)) {
                abnormal.add(current);
            }
        }
        //筛选结果，取涨幅绝对值前5名，按交易额降序
        return TechnologyStockTurnoverRespVo.builder().statisticsDate(dates.get(0))
                .increasingStocks(toRanking(increasing))
                .institutionalStocks(toRanking(institutional))
                .abnormalStocks(toRanking(abnormal)).build();
    }

    private boolean exceeds(StockDailyQuote current, StockDailyQuote base,
                            BigDecimal multiplier) {
        // 分母必须为正，不能把零成交额解释成无限放量。
        return current != null && current.getTurnoverAmountYuan() != null
                && base != null && base.getTurnoverAmountYuan() != null
                && base.getTurnoverAmountYuan().signum() > 0
                && current.getTurnoverAmountYuan().compareTo(base.getTurnoverAmountYuan().multiply(multiplier)) > 0;
    }

    private List<TechnologyStockRankRespVo> toRanking(List<StockDailyQuote> candidates) {
        // 公共条件已保证涨幅为正；先按涨幅取五只，再按成交额重排，不能颠倒。
        List<StockDailyQuote> sorted = candidates.stream()
                .sorted(Comparator.comparing(StockDailyQuote::getChangePercent).reversed()
                        .thenComparing(StockDailyQuote::getStockCode))
                .limit(RANKING_LIMIT)
                .sorted(Comparator.comparing(StockDailyQuote::getTurnoverAmountYuan).reversed()
                        .thenComparing(StockDailyQuote::getStockCode))
                .toList();
        List<TechnologyStockRankRespVo> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            StockDailyQuote quote = sorted.get(i);
            result.add(TechnologyStockRankRespVo.builder().rank(i + 1)
                    .stockCode(quote.getStockCode()).stockName(quote.getStockName()).build());
        }
        return result;
    }
}
