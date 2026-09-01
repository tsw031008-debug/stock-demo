package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单个交易日的市场成交额聚合数据。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyMarketTurnoverDto {

    // 交易日
    private LocalDate tradeDate;
    // 总成交额（元）
    private BigDecimal turnoverAmountYuan;
    // 总记录数
    private Integer totalRecordCount;
    // 成交额大于0的记录数
    private Integer amountRecordCount;
}
