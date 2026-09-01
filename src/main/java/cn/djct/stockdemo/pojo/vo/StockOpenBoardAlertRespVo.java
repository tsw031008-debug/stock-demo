package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 开板提醒响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOpenBoardAlertRespVo {
    // 股票代码
    private String stockCode;
    // 股票名称
    private String stockName;
    // 当前价格
    private BigDecimal currentPrice;
    // 当前涨跌幅
    private BigDecimal currentChangePercent;
    // 今日成交量亿
    private BigDecimal turnoverYi;
}
