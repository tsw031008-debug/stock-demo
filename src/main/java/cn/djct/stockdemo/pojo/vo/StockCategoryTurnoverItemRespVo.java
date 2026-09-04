package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 单个大类的两交易日成交额响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCategoryTurnoverItemRespVo {
    private String categoryCode;
    private String categoryName;
    @Schema(description = "大类内去重后的股票数量")
    private int stockCount;
    @Schema(description = "当前交易日成交额，单位：亿元")
    private BigDecimal currentTurnoverYi;
    @Schema(description = "上一交易日成交额，单位：亿元")
    private BigDecimal previousTurnoverYi;
}
