package cn.djct.stockdemo.pojo.vo;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数MACD背离信号响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDivergenceSignalRespVo {

    @Schema(description = "信号类型：MACD_TOP顶背离、MACD_BOTTOM底背离")
    private IndexDivergenceSignalType signalType;
    @Schema(description = "信号确认时间")
    private LocalDateTime signalTime;
    @Schema(description = "前一有效区间开始时间")
    private LocalDateTime previousIntervalStartTime;
    @Schema(description = "前一有效区间结束时间")
    private LocalDateTime previousIntervalEndTime;
    @Schema(description = "当前有效区间开始时间")
    private LocalDateTime currentIntervalStartTime;
    @Schema(description = "当前有效区间结束时间")
    private LocalDateTime currentIntervalEndTime;
    @Schema(description = "前一有效区间价格极值")
    private BigDecimal previousPriceExtreme;
    @Schema(description = "当前有效区间价格极值")
    private BigDecimal currentPriceExtreme;
    @Schema(description = "前一有效区间MACD极值")
    private BigDecimal previousMacdExtreme;
    @Schema(description = "当前有效区间MACD极值")
    private BigDecimal currentMacdExtreme;
    @Schema(description = "前一有效区间DIF极值")
    private BigDecimal previousDifExtreme;
    @Schema(description = "当前有效区间DIF极值")
    private BigDecimal currentDifExtreme;
}
