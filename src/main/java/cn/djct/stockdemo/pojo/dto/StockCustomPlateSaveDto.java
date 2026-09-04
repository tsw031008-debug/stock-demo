package cn.djct.stockdemo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增或更新自定义子板块请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateSaveDto {

    @Schema(description = "四大类编码", example = "TECHNOLOGY")
    @NotBlank(message = "板块大类编码不能为空")
    private String categoryCode;

    @Schema(description = "自定义子板块名称", example = "人工智能")
    @NotBlank(message = "子板块名称不能为空")
    @Size(max = 128, message = "子板块名称不能超过128个字符")
    private String plateName;

    @Schema(description = "完整成分股代码列表；更新时以本列表替换原有成员")
    @NotNull(message = "成分股列表不能为空")
    @Size(max = 6000, message = "成分股数量不能超过6000")
    private List<@Pattern(regexp = "\\d{6}", message = "股票代码必须为6位数字") String> stockCodes;
}
