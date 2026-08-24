package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 市场成交额原始记录。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketTurnoverRecordDto {

    // 交易日
    private LocalDate tradeDate;
    // 成交额（元）
    private BigDecimal turnoverAmountYuan;
}
