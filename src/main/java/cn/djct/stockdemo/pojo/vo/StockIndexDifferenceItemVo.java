package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.JSONWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 一个累计阈值对应的柱状图数量和饼图占比。 */
@Data
@Builder
public class StockIndexDifferenceItemVo {
    @Schema(description = "差异阈值，单位为百分点；正数表示严格大于，负数表示严格小于")
    private int thresholdPercent;
    @Schema(description = "满足该阈值的累计股票数量，允许与同侧其他阈值重复")
    private int count;
    @Schema(description = "本项数量/同侧四项累计数量之和×100，保留2位小数；分母为0时为null")
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private BigDecimal piePercent;
}
