package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 涨速预警业务数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSpeedAlertDto {

    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal currentChangePercent;
    private BigDecimal slopeDifference;
}
