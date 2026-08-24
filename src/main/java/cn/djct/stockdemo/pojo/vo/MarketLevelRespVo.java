package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 市场水位响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketLevelRespVo {

    @Schema(description = "市场风格", example = "过渡期")
    private String style;

    @Schema(description = "前3个交易日成交额均值，单位：亿元", example = "8000.00")
    private BigDecimal previousThreeDayAverageTurnoverYi;

    @Schema(description = "当前最新两市成交额，单位：亿元", example = "9000.00")
    private BigDecimal currentTurnoverYi;

    @Schema(description = "当前成交额与前5个交易日成交额均值的比值", example = "1.06")
    private BigDecimal volumeRatio;
}
