package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日沪深市场成交额汇总。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketDailyTurnover {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal turnoverAmountYuan;
    private Integer stockCount;
    private Integer amountRecordCount;
    private String dataSource;
    private String dataStatus;
    private LocalDateTime calculatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
