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

    private String stockCode;
    private LocalDate tradeDate;
    private BigDecimal closePrice;
}
