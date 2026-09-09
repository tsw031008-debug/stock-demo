package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单只指数ETF的5日涨幅响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexEtfChangeRespVo {

    @Schema(description = "ETF代码", example = "510050")
    private String etfCode;

    @Schema(description = "对应指数名称", example = "上证50")
    private String indexName;

    @Schema(description = "统计日相对前第5个交易日的涨幅，单位百分比", example = "2.35")
    private BigDecimal changePercent;
}
