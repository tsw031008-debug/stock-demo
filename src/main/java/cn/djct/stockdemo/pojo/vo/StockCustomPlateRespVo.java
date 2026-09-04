package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 自定义子板块响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateRespVo {
    @Schema(description = "子板块编号")
    private Long id;
    @Schema(description = "大类编码")
    private String categoryCode;
    @Schema(description = "大类名称")
    private String categoryName;
    @Schema(description = "子板块名称")
    private String plateName;
}
