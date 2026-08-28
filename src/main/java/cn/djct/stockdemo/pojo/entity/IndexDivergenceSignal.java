package cn.djct.stockdemo.pojo.entity;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数MACD背离信号实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDivergenceSignal {

    /** 数据库主键。 */
    private Long id;

    /** 指数代码。 */
    private String indexCode;

    /** 背离信号类型：顶背离或底背离。 */
    private IndexDivergenceSignalType signalType;

    /** 信号确认时间，即当前有效区间结束时间。 */
    private LocalDateTime signalTime;

    /** 前一有效区间开始时间。 */
    private LocalDateTime previousIntervalStartTime;

    /** 前一有效区间结束时间。 */
    private LocalDateTime previousIntervalEndTime;

    /** 当前有效区间开始时间。 */
    private LocalDateTime currentIntervalStartTime;

    /** 当前有效区间结束时间。 */
    private LocalDateTime currentIntervalEndTime;

    /** 前一有效区间价格极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal previousPriceExtreme;

    /** 当前有效区间价格极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal currentPriceExtreme;

    /** 前一有效区间MACD极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal previousMacdExtreme;

    /** 当前有效区间MACD极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal currentMacdExtreme;

    /** 前一有效区间DIF极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal previousDifExtreme;

    /** 当前有效区间DIF极值：顶背离取最高值，底背离取最低值。 */
    private BigDecimal currentDifExtreme;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
