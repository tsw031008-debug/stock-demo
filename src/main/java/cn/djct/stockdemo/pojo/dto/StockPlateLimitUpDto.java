package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 板块涨停统计业务数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateLimitUpDto {

    // 板块名称
    private String plateName;
    // 总股票数
    private Integer totalStockCount;
    // 板块涨跌幅
    private BigDecimal plateChangePercent;
    // 涨停股票数
    private Integer limitUpStockCount;
    // 涨停占比
    private BigDecimal limitUpRatio;
}
