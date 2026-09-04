package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 单个大类的两交易日成交额对比数据。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCategoryTurnoverItemDto {
    private String categoryCode;
    private String categoryName;
    private int stockCount;
    private BigDecimal currentTurnoverYi;
    private BigDecimal previousTurnoverYi;
}
