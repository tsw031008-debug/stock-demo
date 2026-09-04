package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexStrengthType;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.pojo.dto.IndexDailyStyleDto;
import cn.djct.stockdemo.pojo.dto.IndexStyleItemDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 四个固定指数的单日涨幅和五日强弱计算组件。
 */
@Component
public class IndexStyleCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CHANGE_SCALE = 2;

    /**
     * 按交易日分别比较四个指数，标记每日唯一的最强和最弱指数。
     *
     * <p>四个指数完全同涨幅时均不标记；部分并列时按产品固定展示顺序取首个。</p>
     *
     * @param tradeDates 最近五个交易日，按升序排列
     * @param quotes 四个指数在五个交易日的完整行情
     * @return 按产品固定指数顺序排列的五日结果
     */
    public List<IndexStyleItemDto> calculate(
            List<LocalDate> tradeDates,
            List<IndexDailyQuote> quotes
    ) {
        if (tradeDates == null || tradeDates.size() != 5) {
            throw new IllegalArgumentException("指数风格计算必须包含5个交易日");
        }

        //计算每日对应的指数涨幅
        Map<LocalDate, Map<String, BigDecimal>> changesByDate = indexChanges(tradeDates, quotes);
        //标记强弱map
        Map<String, List<IndexDailyStyleDto>> stylesByCode = new HashMap<>();

        for (IndexStyleIndex index : IndexStyleIndex.values()) {
            //按照指数代码初始化强弱列表
            stylesByCode.put(index.getIndexCode(), new ArrayList<>(tradeDates.size()));
        }

        //按照日期遍历，标记强弱
        for (LocalDate tradeDate : tradeDates) {
            //获得该日对应的四个指数涨幅
            Map<String, BigDecimal> dailyChanges = changesByDate.get(tradeDate);
            //找到最强和最弱指数代码
            String strongestCode = findExtremeCode(dailyChanges, true);
            String weakestCode = findExtremeCode(dailyChanges, false);
            //判断最强和最弱指数是否相同
            boolean allEqual = dailyChanges.get(strongestCode)
                    .compareTo(dailyChanges.get(weakestCode)) == 0;

            //按照指数代码标记强弱
            for (IndexStyleIndex index : IndexStyleIndex.values()) {
                IndexStrengthType strengthType = IndexStrengthType.NONE;
                if (!allEqual && index.getIndexCode().equals(strongestCode)) {
                    strengthType = IndexStrengthType.STRONG;
                } else if (!allEqual && index.getIndexCode().equals(weakestCode)) {
                    strengthType = IndexStrengthType.WEAK;
                }
                stylesByCode.get(index.getIndexCode()).add(IndexDailyStyleDto.builder()
                        .tradeDate(tradeDate)
                        .changePercent(dailyChanges.get(index.getIndexCode()))
                        .strengthType(strengthType)
                        .build());
            }
        }

        List<IndexStyleItemDto> result = new ArrayList<>(IndexStyleIndex.values().length);
        for (IndexStyleIndex index : IndexStyleIndex.values()) {
            result.add(IndexStyleItemDto.builder()
                    .indexCode(index.getIndexCode())
                    .indexName(index.getIndexName())
                    .dailyStyles(stylesByCode.get(index.getIndexCode()))
                    .build());
        }
        return result;
    }

    /**
     * 将行情转成“交易日-指数代码-涨幅”索引，并守住20个数据点的完整性。
     */
    private Map<LocalDate, Map<String, BigDecimal>> indexChanges(
            List<LocalDate> tradeDates,
            List<IndexDailyQuote> quotes
    ) {
        if (quotes == null) {
            throw new IllegalArgumentException("指数日行情不能为空");
        }
        Map<LocalDate, Map<String, BigDecimal>> result = new HashMap<>();
        // 遍历行情数据，计算涨幅
        for (IndexDailyQuote quote : quotes) {
            if (!tradeDates.contains(quote.getTradeDate())) {
                throw new IllegalStateException("指数行情包含范围外交易日：" + quote.getTradeDate());
            }
            // 校验指数代码
            IndexStyleIndex.fromCode(quote.getIndexCode());
            // 计算涨幅
            BigDecimal changePercent = calculateChangePercent(quote);
            // 按照日期和指数代码存储涨幅
            BigDecimal previous = result.computeIfAbsent(quote.getTradeDate(), key -> new HashMap<>())
                    .put(quote.getIndexCode(), changePercent);
            if (previous != null) {
                throw new IllegalStateException("指数日行情重复，indexCode=" + quote.getIndexCode()
                        + "，tradeDate=" + quote.getTradeDate());
            }
        }

        //校验每日的每个知识数据是否完整
        for (LocalDate tradeDate : tradeDates) {
            Map<String, BigDecimal> dailyChanges = result.get(tradeDate);
            if (dailyChanges == null || dailyChanges.size() != IndexStyleIndex.values().length) {
                throw new IllegalStateException("指数日行情数据不完整，tradeDate=" + tradeDate);
            }
            for (IndexStyleIndex index : IndexStyleIndex.values()) {
                if (!dailyChanges.containsKey(index.getIndexCode())) {
                    throw new IllegalStateException("指数日行情数据不完整，tradeDate=" + tradeDate
                            + "，indexCode=" + index.getIndexCode());
                }
            }
        }
        return result;
    }

    /**
     * 涨幅口径：（当日收盘价÷前一交易日收盘价-1）×100%，单位为百分比。
     */
    private BigDecimal calculateChangePercent(IndexDailyQuote quote) {
        if (quote.getClosePrice() == null || quote.getClosePrice().signum() <= 0
                || quote.getPreviousClosePrice() == null
                || quote.getPreviousClosePrice().signum() <= 0) {
            throw new IllegalStateException("指数收盘价不完整，indexCode=" + quote.getIndexCode()
                    + "，tradeDate=" + quote.getTradeDate());
        }
        return quote.getClosePrice()
                .divide(quote.getPreviousClosePrice(), 8, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(ONE_HUNDRED)
                .setScale(CHANGE_SCALE, RoundingMode.HALF_UP);
    }

    private String findExtremeCode(Map<String, BigDecimal> changes, boolean maximum) {
        IndexStyleIndex selected = IndexStyleIndex.values()[0];
        for (IndexStyleIndex index : IndexStyleIndex.values()) {
            int comparison = changes.get(index.getIndexCode())
                    .compareTo(changes.get(selected.getIndexCode()));
            if ((maximum && comparison > 0) || (!maximum && comparison < 0)) {
                selected = index;
            }
        }
        return selected.getIndexCode();
    }
}
