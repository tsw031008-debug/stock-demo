package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票日行情实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockDailyQuote {

    private Long id;
    private String stockCode;
    private String stockName;
    private LocalDate tradeDate;
    private LocalDateTime quoteTime;
    private BigDecimal closePrice;
    private BigDecimal previousClosePrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volumeHand;
    private BigDecimal turnoverAmountYuan;
    private BigDecimal bid1Price;
    private Long bid1VolumeHand;
    private BigDecimal ask1Price;
    private Long ask1VolumeHand;
    private BigDecimal changePercent;
    private BigDecimal amplitudePercent;
    private BigDecimal turnoverRate;
    private BigDecimal peRatio;
    private BigDecimal pbRatio;
    private BigDecimal circulatingMarketCapYuan;
    private BigDecimal totalMarketCapYuan;
    private String dataSource;
    private String dataStatus;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
