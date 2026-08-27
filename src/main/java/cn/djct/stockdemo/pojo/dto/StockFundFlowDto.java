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

    // 交易日期
    private LocalDate tradeDate;
    // 股票代码
    private String stockCode;
    // 股票名称
    private String stockName;
    // 最新价
    private BigDecimal latestPrice;
    // 涨跌幅
    private BigDecimal changePercent;
    // 主力净流入金额
    private BigDecimal mainNetInflowYuan;
    // 主力净流入占比
    private BigDecimal mainNetInflowRatio;
    // 超大单净流入金额
    private BigDecimal superLargeNetInflowYuan;
    // 超大单净流入占比
    private BigDecimal superLargeNetInflowRatio;
    // 大单净流入金额
    private BigDecimal largeNetInflowYuan;
    // 大单净流入占比
    private BigDecimal largeNetInflowRatio;
}
