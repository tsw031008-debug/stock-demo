package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 入选时的股票名称及指定交易日行情。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeftSideStockRespVo {
    private String stockCode;
    private String stockName;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    @Schema(description = "当日涨幅，百分数，例如5表示5%")
    private BigDecimal changePercent;
    @Schema(description = "当日成交额，元")
    private BigDecimal turnoverAmountYuan;
    @Schema(description = "相对前3个交易日收盘价的涨幅，百分数，保留2位小数")
    private BigDecimal threeDayChangePercent;
    @Schema(description = "相对前5个交易日收盘价的涨幅，百分数，保留2位小数")
    private BigDecimal fiveDayChangePercent;
    @Schema(description = "相对前10个交易日收盘价的涨幅，百分数，保留2位小数")
    private BigDecimal tenDayChangePercent;
}
