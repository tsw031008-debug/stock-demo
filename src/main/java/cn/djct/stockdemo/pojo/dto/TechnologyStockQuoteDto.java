package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 科技股榜单计算使用的股票日行情原始数据。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockQuoteDto {

    private String stockCode;
    private String stockName;
    private LocalDate tradeDate;
    private BigDecimal closePrice;
    private BigDecimal turnoverAmountYuan;
    private BigDecimal changePercent;
}
