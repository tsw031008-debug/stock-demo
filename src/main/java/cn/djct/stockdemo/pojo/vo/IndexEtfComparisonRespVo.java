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
 * 四只固定指数ETF的5日涨幅比较响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexEtfComparisonRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "最新完整交易日", example = "2026-09-04")
    private LocalDate statisticsDate;

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "5日涨幅基准日，即统计日前第5个交易日", example = "2026-08-28")
    private LocalDate baseTradeDate;

    @Schema(description = "按上证50、沪深300、创业板50、中证1000固定顺序返回")
    private List<IndexEtfChangeRespVo> etfs;
}
