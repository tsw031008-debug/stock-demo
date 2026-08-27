package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 板块涨停统计响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateLimitUpRespVo {

    private String plateName;
    private Integer totalStockCount;
    private BigDecimal plateChangePercent;
    private Integer limitUpStockCount;
    private BigDecimal limitUpRatio;
}
