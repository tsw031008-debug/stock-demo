package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.TechnologyStockQuoteDto;
import cn.djct.stockdemo.pojo.dto.TechnologyStockRankDto;
import cn.djct.stockdemo.pojo.dto.TechnologyStockRankingDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 热门和潜力科技股榜单计算组件。
 */
@Component
public class TechnologyStockRankingCalculator {

    private static final int RANKING_LIMIT = 5;
    private static final BigDecimal FIFTEEN_PERCENT_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal ONE_PERCENT_MULTIPLIER = new BigDecimal("1.01");
    private static final BigDecimal FIVE_PERCENT_MULTIPLIER = new BigDecimal("1.05");
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");

    /**
     * 使用六个交易日的日行情计算两组科技股前五名。
     * 潜力榜边界均为严格大于或严格小于，并按统计日当日涨幅绝对值排序。
     */
    public TechnologyStockRankingDto calculate(
            LocalDate statisticsDate,
            LocalDate fiveDayBaseDate,
            LocalDate tenDayBaseDate,
            LocalDate fifteenDayBaseDate,
            LocalDate twentyDayBaseDate,
            LocalDate sixtyDayBaseDate,
            List<TechnologyStockQuoteDto> quotes
    ) {
        //用set集合存储合格目标日期
        Set<LocalDate> targetDates = validateAndBuildTargetDates(
                statisticsDate,
                fiveDayBaseDate,
                tenDayBaseDate,
                fifteenDayBaseDate,
                twentyDayBaseDate,
                sixtyDayBaseDate
        );
        Objects.requireNonNull(quotes, "科技股日行情不能为空");
        //按股票代码和目标日期分组
        Map<String, Map<LocalDate, TechnologyStockQuoteDto>> quotesByStock = groupQuotes(
                quotes,
                targetDates
        );

        List<RankCandidate> hotCandidates = new ArrayList<>();
        List<RankCandidate> potentialCandidates = new ArrayList<>();
        //遍历每只股票对应的目标日期行情
        for (Map<LocalDate, TechnologyStockQuoteDto> quoteByDate : quotesByStock.values()) {
            //获得当前交易日行情
            TechnologyStockQuoteDto current = quoteByDate.get(statisticsDate);
            if (!isCommonCandidate(current)) {
                continue;
            }

            //获得20日的行情
            TechnologyStockQuoteDto twentyDayBase = quoteByDate.get(twentyDayBaseDate);
            //进行收盘价非法校验
            if (hasPositiveClose(twentyDayBase)) {
                //计算20日涨幅，保留34位数字，向最近值舍入
                BigDecimal twentyDayRatio = current.getClosePrice().divide(
                        twentyDayBase.getClosePrice(),
                        MathContext.DECIMAL128
                );
                //将当前股票加入热门候选列表
                hotCandidates.add(candidate(current, twentyDayRatio));
            }

            //判断当前股票是否为潜力候选
            if (isPotentialCandidate(
                    current,
                    quoteByDate.get(fiveDayBaseDate),
                    quoteByDate.get(tenDayBaseDate),
                    quoteByDate.get(fifteenDayBaseDate),
                    twentyDayBase,
                    quoteByDate.get(sixtyDayBaseDate)
            )) {
                //是潜力股，加入潜力候选列表，保存当前涨幅绝对值
                potentialCandidates.add(candidate(current, current.getChangePercent().abs()));
            }
        }

        return TechnologyStockRankingDto.builder()
                .statisticsDate(statisticsDate)
                .hotStocks(toRanking(hotCandidates))
                .potentialStocks(toRanking(potentialCandidates))
                .build();
    }

    private Set<LocalDate> validateAndBuildTargetDates(
            LocalDate statisticsDate,
            LocalDate fiveDayBaseDate,
            LocalDate tenDayBaseDate,
            LocalDate fifteenDayBaseDate,
            LocalDate twentyDayBaseDate,
            LocalDate sixtyDayBaseDate
    ) {
        Objects.requireNonNull(statisticsDate, "统计交易日不能为空");
        List<LocalDate> baseDates = List.of(
                Objects.requireNonNull(fiveDayBaseDate, "T-5交易日不能为空"),
                Objects.requireNonNull(tenDayBaseDate, "T-10交易日不能为空"),
                Objects.requireNonNull(fifteenDayBaseDate, "T-15交易日不能为空"),
                Objects.requireNonNull(twentyDayBaseDate, "T-20交易日不能为空"),
                Objects.requireNonNull(sixtyDayBaseDate, "T-60交易日不能为空")
        );
        Set<LocalDate> targetDates = new HashSet<>(baseDates);
        targetDates.add(statisticsDate);
        if (targetDates.size() != 6 || baseDates.stream().anyMatch(date -> !date.isBefore(statisticsDate))) {
            throw new IllegalArgumentException("科技股榜单交易日范围无效");
        }
        return targetDates;
    }

    private Map<String, Map<LocalDate, TechnologyStockQuoteDto>> groupQuotes(
            List<TechnologyStockQuoteDto> quotes,
            Set<LocalDate> targetDates
    ) {
        Map<String, Map<LocalDate, TechnologyStockQuoteDto>> quotesByStock = new HashMap<>();
        for (TechnologyStockQuoteDto quote : quotes) {
            if (quote == null || quote.getStockCode() == null || quote.getStockCode().isBlank()
                    || quote.getTradeDate() == null || !targetDates.contains(quote.getTradeDate())) {
                throw new IllegalStateException("科技股日行情存在非法记录");
            }
            Map<LocalDate, TechnologyStockQuoteDto> quoteByDate = quotesByStock.computeIfAbsent(
                    quote.getStockCode(),
                    ignored -> new HashMap<>()
            );
            if (quoteByDate.put(quote.getTradeDate(), quote) != null) {
                throw new IllegalStateException("科技股日行情重复，stockCode="
                        + quote.getStockCode() + "，tradeDate=" + quote.getTradeDate());
            }
        }
        return quotesByStock;
    }

    private boolean isCommonCandidate(TechnologyStockQuoteDto current) {
        if (current == null || current.getStockName() == null || current.getStockName().isBlank()
                || !isPositive(current.getClosePrice())) {
            return false;
        }
        return !current.getStockName().contains("ST");
    }

    private boolean isPotentialCandidate(
            TechnologyStockQuoteDto current,
            TechnologyStockQuoteDto fiveDayBase,
            TechnologyStockQuoteDto tenDayBase,
            TechnologyStockQuoteDto fifteenDayBase,
            TechnologyStockQuoteDto twentyDayBase,
            TechnologyStockQuoteDto sixtyDayBase
    ) {
        //非法校验
        if (current.getChangePercent() == null
                || current.getTurnoverAmountYuan() == null
                || current.getTurnoverAmountYuan().compareTo(ONE_HUNDRED_MILLION) <= 0
                || !hasPositiveClose(fiveDayBase)
                || !hasPositiveClose(tenDayBase)
                || !hasPositiveClose(fifteenDayBase)
                || !hasPositiveClose(twentyDayBase)
                || !hasPositiveClose(sixtyDayBase)) {
            return false;
        }

        BigDecimal currentClose = current.getClosePrice();
        //15日涨幅小于 15%， 10日涨幅大于1% ，5日涨幅大于 0, 20日涨幅大于5% ，60日涨幅大于0，成交额大于1亿，
        return currentClose.compareTo(
                fifteenDayBase.getClosePrice().multiply(FIFTEEN_PERCENT_MULTIPLIER)
        ) < 0
                && currentClose.compareTo(
                tenDayBase.getClosePrice().multiply(ONE_PERCENT_MULTIPLIER)
        ) > 0
                && currentClose.compareTo(fiveDayBase.getClosePrice()) > 0
                && currentClose.compareTo(
                twentyDayBase.getClosePrice().multiply(FIVE_PERCENT_MULTIPLIER)
        ) > 0
                && currentClose.compareTo(sixtyDayBase.getClosePrice()) > 0;
    }

    private boolean hasPositiveClose(TechnologyStockQuoteDto quote) {
        return quote != null && isPositive(quote.getClosePrice());
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    //返回股票代码，名称，涨幅
    private RankCandidate candidate(TechnologyStockQuoteDto quote, BigDecimal score) {
        return new RankCandidate(
                quote.getStockCode(),
                quote.getStockName(),
                score
        );
    }

    //将候选列表转换为榜单列表
    private List<TechnologyStockRankDto> toRanking(List<RankCandidate> candidates) {
        //从大到小，相同涨幅按股票代码排序，取前5
        List<RankCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparing(RankCandidate::score).reversed()
                        .thenComparing(RankCandidate::stockCode))
                .limit(RANKING_LIMIT)
                .toList();
        List<TechnologyStockRankDto> result = new ArrayList<>(sorted.size());
        //转换返回格式、补上排名。
        for (int index = 0; index < sorted.size(); index++) {
            RankCandidate candidate = sorted.get(index);
            result.add(TechnologyStockRankDto.builder()
                    .rank(index + 1)
                    .stockCode(candidate.stockCode())
                    .stockName(candidate.stockName())
                    .build());
        }
        return List.copyOf(result);
    }

    private record RankCandidate(String stockCode, String stockName, BigDecimal score) {
    }
}
