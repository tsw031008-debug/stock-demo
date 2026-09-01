package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票资金流向查询响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票资金流向")
public class StockFundFlowRespVo {

    @Schema(description = "交易日期", example = "2026-08-25")
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate tradeDate;
    @Schema(description = "股票代码", example = "600519")
    private String stockCode;
    @Schema(description = "股票名称", example = "贵州茅台")
    private String stockName;
    @Schema(description = "收盘价或最新价")
    private BigDecimal latestPrice;
    @Schema(description = "涨跌幅，单位：百分比")
    private BigDecimal changePercent;
    @Schema(description = "主力净流入金额，单位：元")
    private BigDecimal mainNetInflowYuan;
    @Schema(description = "主力净流入占比，单位：百分比")
    private BigDecimal mainNetInflowRatio;
    @Schema(description = "超大单净流入金额，单位：元")
    private BigDecimal superLargeNetInflowYuan;
    @Schema(description = "超大单净流入占比，单位：百分比")
    private BigDecimal superLargeNetInflowRatio;
    @Schema(description = "大单净流入金额，单位：元")
    private BigDecimal largeNetInflowYuan;
    @Schema(description = "大单净流入占比，单位：百分比")
    private BigDecimal largeNetInflowRatio;
}
