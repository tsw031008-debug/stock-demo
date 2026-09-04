package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个指数的五日强弱响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexStyleItemRespVo {

    @Schema(description = "指数代码", example = "000016")
    private String indexCode;

    @Schema(description = "指数名称", example = "上证50")
    private String indexName;

    @Schema(description = "按交易日升序排列的五日强弱结果")
    private List<IndexDailyStyleRespVo> dailyStyles;
}
