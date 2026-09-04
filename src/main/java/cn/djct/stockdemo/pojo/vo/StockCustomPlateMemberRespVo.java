package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 自定义子板块成分股响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateMemberRespVo {
    @Schema(description = "股票代码")
    private String stockCode;
    @Schema(description = "股票名称")
    private String stockName;
}
