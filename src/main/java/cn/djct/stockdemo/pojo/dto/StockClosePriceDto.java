package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票历史收盘价原始数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockClosePriceDto {

    // 股票代码
    private String stockCode;
    // 交易日期
    private LocalDate tradeDate;
    // 收盘价
    private BigDecimal closePrice;
}
