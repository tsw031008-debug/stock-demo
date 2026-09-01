package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 市场水位计算结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketLevelDto {

    // 品种
    private String style;
    // 3日均成交量亿
    private BigDecimal previousThreeDayAverageTurnoverYi;
    // 今日成交量亿
    private BigDecimal currentTurnoverYi;
    // 成交量占比
    private BigDecimal volumeRatio;
}
