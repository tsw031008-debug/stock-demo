package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 四大类响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomCategoryRespVo {
    @Schema(description = "大类编码")
    private String categoryCode;
    @Schema(description = "大类名称")
    private String categoryName;
}
