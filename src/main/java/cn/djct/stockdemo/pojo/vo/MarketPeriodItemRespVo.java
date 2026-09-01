package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 市场周期平均成交额响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPeriodItemRespVo {

    @Schema(description = "比较类型：YOY同比、MOM环比、CURRENT当前")
    private String comparisonType;
    @Schema(description = "周期标签")
    private String periodLabel;
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate startDate;
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate endDate;
    private Integer tradingDayCount;
    @Schema(description = "日均成交额，单位：亿元")
    private BigDecimal averageTurnoverYi;
    private Boolean available;
}
