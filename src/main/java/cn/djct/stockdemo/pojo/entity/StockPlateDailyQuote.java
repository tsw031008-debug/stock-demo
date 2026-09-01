package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票板块日线实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateDailyQuote {

    private Long id;
    private Long plateId;
    private String plateName;
    private LocalDate tradeDate;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal changePercent;
    private BigDecimal turnoverAmountYuan;
    private Integer stockCount;
    private String dataSource;
    private String dataStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
