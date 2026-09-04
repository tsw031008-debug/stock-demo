package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票指定交易日成交额原始数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTurnoverByDateDto {

    private String stockCode;
    private LocalDate tradeDate;
    private BigDecimal turnoverAmountYuan;
}
