package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 最新指数曲线和背离信号响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDivergenceLatestRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "数据所属交易日", example = "2026-08-28")
    private LocalDate tradeDate;

    @Schema(description = "上证指数昨收横线价格", example = "3820.10")
    private BigDecimal previousClosePrice;

    @Schema(description = "上证指数分钟曲线，按行情时间升序")
    private List<IndexMinuteCurvePointRespVo> curveData;

    @Schema(description = "MACD顶背离和底背离信号，按确认时间升序")
    private List<IndexDivergenceSignalRespVo> signalData;
}
