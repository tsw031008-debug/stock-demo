package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 涨速预警响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSpeedAlertRespVo {

    // 股票代码
    private String stockCode;
    // 股票名称
    private String stockName;
    // 当前价格
    private BigDecimal currentPrice;
    // 当前涨跌幅
    private BigDecimal currentChangePercent;
}
