package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指数分钟行情实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexMinuteQuote {

    private Long id;
    private String indexCode;
    private String indexName;
    private LocalDate tradeDate;
    private LocalDateTime quoteTime;
    private BigDecimal currentPrice;
    private BigDecimal previousClosePrice;
    private String dataSource;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
