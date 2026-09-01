package cn.djct.stockdemo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数分钟曲线点响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMinuteCurvePointRespVo {

    @Schema(description = "行情分钟，上海时区", example = "2026-08-28T09:31:00")
    private LocalDateTime quoteTime;

    @Schema(description = "上证指数当前价格", example = "3850.12")
    private BigDecimal currentPrice;
}
