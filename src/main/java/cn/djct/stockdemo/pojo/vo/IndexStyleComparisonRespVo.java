package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数大小风格五日比较响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexStyleComparisonRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "最新完整交易日", example = "2026-09-03")
    private LocalDate statisticsDate;

    @Schema(description = "按升序排列的最近五个交易日")
    private List<String> tradeDates;

    @Schema(description = "按上证50、上证指数、创业板综指、中证1000固定顺序返回")
    private List<IndexStyleItemRespVo> indices;
}
