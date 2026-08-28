package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 指数分钟MACD计算组件，参数固定为12、26、9。
 */
@Component
public class IndexMacdCalculator {

    private static final int FAST_PERIOD = 12;
    private static final int SLOW_PERIOD = 26;
    private static final int SIGNAL_PERIOD = 9;
    private static final int CALCULATION_SCALE = 16;
    private static final int RESULT_SCALE = 8;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /**
     * 按行情时间升序计算DIF、DEA和MACD柱值。
     * 首个价格用于初始化EMA12和EMA26，首个DIF用于初始化DEA。
     *
     * @param quotes 同一指数按分钟升序排列的行情
     * @return 与输入行情数量和顺序一致的MACD结果
     */
    public List<IndexMacdDto> calculate(List<IndexMinuteQuote> quotes) {
        Objects.requireNonNull(quotes, "指数分钟行情不能为空");
        if (quotes.isEmpty()) {
            return List.of();
        }
        // 校验行情
        validateQuotes(quotes);

        List<IndexMacdDto> result = new ArrayList<>(quotes.size());
        BigDecimal previousEma12 = null;
        BigDecimal previousEma26 = null;
        BigDecimal previousDea = null;
        for (IndexMinuteQuote quote : quotes) {
            BigDecimal currentPrice = quote.getCurrentPrice();
            BigDecimal ema12;
            BigDecimal ema26;
            BigDecimal dif;
            BigDecimal dea;
            if (previousEma12 == null) {
                ema12 = currentPrice;
                ema26 = ema12;
                dif = BigDecimal.ZERO;
                dea = dif;
            } else {
                // 计算EMA12、EMA26、DIF和DEA
                ema12 = calculateEma(currentPrice, previousEma12, FAST_PERIOD);
                ema26 = calculateEma(currentPrice, previousEma26, SLOW_PERIOD);
                dif = ema12.subtract(ema26);
                dea = calculateEma(dif, previousDea, SIGNAL_PERIOD);
            }
            // 计算MACD柱值
            BigDecimal macd = dif.subtract(dea).multiply(TWO);
            result.add(IndexMacdDto.builder()
                    .quoteTime(quote.getQuoteTime())
                    .currentPrice(currentPrice)
                    .dif(toResultScale(dif))
                    .dea(toResultScale(dea))
                    .macd(toResultScale(macd))
                    .build());
            previousEma12 = ema12;
            previousEma26 = ema26;
            previousDea = dea;
        }
        return List.copyOf(result);
    }

    /**
     * EMA(N) = 前EMA × (N-1)/(N+1) + 当前值 × 2/(N+1)。
     */
    private BigDecimal calculateEma(BigDecimal currentValue, BigDecimal previousEma, int period) {
        return previousEma.multiply(BigDecimal.valueOf(period - 1L))
                .add(currentValue.multiply(TWO))
                .divide(BigDecimal.valueOf(period + 1L), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal toResultScale(BigDecimal value) {
        return value.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 校验行情完整性、单一指数和严格递增的时间顺序。
     */
    private void validateQuotes(List<IndexMinuteQuote> quotes) {
        String indexCode = null;
        LocalDateTime previousQuoteTime = null;
        for (IndexMinuteQuote quote : quotes) {
            if (quote == null || quote.getIndexCode() == null || quote.getIndexCode().isBlank()
                    || quote.getQuoteTime() == null || quote.getCurrentPrice() == null) {
                throw new IllegalArgumentException("指数分钟行情字段不完整");
            }
            if (quote.getCurrentPrice().signum() <= 0) {
                throw new IllegalArgumentException("指数分钟价格必须大于0，quoteTime=" + quote.getQuoteTime());
            }
            if (indexCode == null) {
                indexCode = quote.getIndexCode();
            } else if (!indexCode.equals(quote.getIndexCode())) {
                throw new IllegalArgumentException("MACD计算只能包含同一指数行情");
            }
            if (previousQuoteTime != null && !quote.getQuoteTime().isAfter(previousQuoteTime)) {
                throw new IllegalArgumentException("指数分钟行情必须按时间严格升序排列");
            }
            previousQuoteTime = quote.getQuoteTime();
        }
    }
}
