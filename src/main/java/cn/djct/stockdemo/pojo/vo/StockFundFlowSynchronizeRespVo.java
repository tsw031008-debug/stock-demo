package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 股票资金流向同步响应。
 */
@Data
@Builder
@Schema(description = "股票资金流向同步响应")
public class StockFundFlowSynchronizeRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "交易日期", example = "2026-08-21")
    private LocalDate tradeDate;

    @Schema(description = "保存数量；非交易日或当天已完整同步时为0", example = "5200")
    private Integer savedCount;
}
