package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票资金流向查询业务数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFundFlowDto {

    private LocalDate tradeDate;
    private String stockCode;
    private String stockName;
    private BigDecimal latestPrice;
    private BigDecimal changePercent;
    private BigDecimal mainNetInflowYuan;
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetInflowYuan;
    private BigDecimal superLargeNetInflowRatio;
    private BigDecimal largeNetInflowYuan;
    private BigDecimal largeNetInflowRatio;
}
