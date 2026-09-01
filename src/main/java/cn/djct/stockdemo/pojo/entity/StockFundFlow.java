package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票资金流向实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockFundFlow {

    private Long id;
    private String stockCode;
    private String stockName;
    private LocalDate tradeDate;
    private BigDecimal latestPrice;
    private BigDecimal changePercent;
    private BigDecimal mainNetInflowYuan;
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetInflowYuan;
    private BigDecimal superLargeNetInflowRatio;
    private BigDecimal largeNetInflowYuan;
    private BigDecimal largeNetInflowRatio;
    private String dataSource;
    private String dataStatus;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
