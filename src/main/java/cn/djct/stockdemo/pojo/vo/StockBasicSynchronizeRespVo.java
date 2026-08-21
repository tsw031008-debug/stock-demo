package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 股票基础信息同步响应。
 */
@Data
@Builder
@Schema(description = "股票基础信息同步响应")
public class StockBasicSynchronizeRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "同步日期", example = "2026-08-20")
    private LocalDate tradeDate;

    @Schema(description = "保存数量；非交易日或当天已同步时为0", example = "5200")
    private Integer savedCount;
}
