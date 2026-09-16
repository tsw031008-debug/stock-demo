package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceItemVo;
import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceRespVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 使用两交易日未复权收盘价，统计股票相对沪深300的累计涨幅差异。 */
@Component
public class StockIndexDifferenceCalculator {
    private static final int[] THRESHOLDS = {1, 3, 5, 7};

    /**
     * 差异为股票T/P收盘价比值减指数T/P收盘价比值；严格比较，阈值单位为百分点。
     * 候选范围为T日已落库股票，缺少有效价格的股票跳过；饼图分母为同侧四项累计计数之和。
     */
    public StockIndexDifferenceRespVo calculate(LocalDate tradeDate, LocalDate previousTradeDate,
            BigDecimal indexClose, BigDecimal indexPreviousClose, List<StockClosePriceDto> quotes) {
        if (!positive(indexClose) || !positive(indexPreviousClose)) {
            throw new IllegalStateException("沪深300两日收盘价必须大于0");
        }
        Map<String, BigDecimal> current = new HashMap<>();
        Map<String, BigDecimal> previous = new HashMap<>();
        // 遍历个股，分别记录T日和T-1日的收盘价
        for (StockClosePriceDto quote : quotes) {
            if (tradeDate.equals(quote.getTradeDate())) {
                current.put(quote.getStockCode(), quote.getClosePrice());
            } else if (previousTradeDate.equals(quote.getTradeDate())) {
                previous.put(quote.getStockCode(), quote.getClosePrice());
            }
        }
        if (current.isEmpty() || previous.isEmpty()) {
            throw new IllegalStateException("股票两日收盘行情尚未就绪");
        }
        int[] strong = new int[THRESHOLDS.length];
        int[] weak = new int[THRESHOLDS.length];
        int validCount = 0;
        for (Map.Entry<String, BigDecimal> entry : current.entrySet()) {
            BigDecimal stockClose = entry.getValue();
            BigDecimal stockPreviousClose = previous.get(entry.getKey());
            if (!positive(stockClose) || !positive(stockPreviousClose)) {
                continue;
            }
            validCount++;
            // 分母均为正，交叉相乘进行精确比较，避免除法舍入使边界股票误入统计。
            BigDecimal numerator = stockClose.multiply(indexPreviousClose)
                    .subtract(stockPreviousClose.multiply(indexClose));
            BigDecimal denominator = stockPreviousClose.multiply(indexPreviousClose);
            //
            for (int i = 0; i < THRESHOLDS.length; i++) {
                //根据阈值1，3，5，7计算边界
                BigDecimal boundary = denominator.multiply(BigDecimal.valueOf(THRESHOLDS[i], 2));
                //比较阈值，对应的强弱计数加一
                if (numerator.compareTo(boundary) > 0) {
                    strong[i]++;
                }
                //对阈值取反
                if (numerator.compareTo(boundary.negate()) < 0) {
                    weak[i]++;
                }
            }
        }
        return StockIndexDifferenceRespVo.builder()
                .tradeDate(tradeDate).previousTradeDate(previousTradeDate)
                .indexCode(IndexStyleIndex.CSI_300.getIndexCode())
                .totalStockCount(current.size()).validStockCount(validCount)
                .skippedStockCount(current.size() - validCount)
                .strong(items(strong, 1)).weak(items(weak, -1)).build();
    }

    private List<StockIndexDifferenceItemVo> items(int[] counts, int sign) {
        // 计算总和
        long total = 0;
        for (int count : counts) {
            total += count;
        }
        List<StockIndexDifferenceItemVo> result = new ArrayList<>(THRESHOLDS.length);
        for (int i = 0; i < THRESHOLDS.length; i++) {
            //计算占比
            BigDecimal percent = total == 0 ? null : BigDecimal.valueOf(counts[i])
                    .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            result.add(StockIndexDifferenceItemVo.builder().thresholdPercent(sign * THRESHOLDS[i])
                    .count(counts[i]).piePercent(percent).build());
        }
        return result;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
