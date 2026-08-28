package cn.djct.stockdemo.pojo.dto;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数MACD背离计算结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDivergenceSignalDto {

    private IndexDivergenceSignalType signalType;
    private LocalDateTime signalTime;
    private LocalDateTime previousIntervalStartTime;
    private LocalDateTime previousIntervalEndTime;
    private LocalDateTime currentIntervalStartTime;
    private LocalDateTime currentIntervalEndTime;
    private BigDecimal previousPriceExtreme;
    private BigDecimal currentPriceExtreme;
    private BigDecimal previousMacdExtreme;
    private BigDecimal currentMacdExtreme;
    private BigDecimal previousDifExtreme;
    private BigDecimal currentDifExtreme;
}
