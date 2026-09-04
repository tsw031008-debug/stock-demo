package cn.djct.stockdemo.pojo.dto;

import cn.djct.stockdemo.constant.IndexStrengthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单个指数在一个交易日的涨幅和强弱标记。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexDailyStyleDto {

    private LocalDate tradeDate;
    private BigDecimal changePercent;
    private IndexStrengthType strengthType;
}
