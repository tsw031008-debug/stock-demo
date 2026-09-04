package cn.djct.stockdemo.pojo.vo;

import cn.djct.stockdemo.constant.IndexStrengthType;
import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指数单日强弱响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexDailyStyleRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "交易日", example = "2026-09-03")
    private LocalDate tradeDate;

    @Schema(description = "当日涨幅，单位：%", example = "1.25")
    private BigDecimal changePercent;

    @Schema(description = "强弱标记：STRONG强、WEAK弱、NONE不标记", example = "STRONG")
    private IndexStrengthType strengthType;
}
