package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 市场周期平均成交额。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPeriodItemDto {

    private String comparisonType;
    private String periodLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer tradingDayCount;
    private BigDecimal averageTurnoverYi;
    private Boolean available;
}
