package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 前后第N个交易日查询响应。
 */
@Data
@Builder
@Schema(description = "前后第N个交易日查询响应")
public class TradingDayOffsetRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "基准日期", example = "2023-06-26")
    private LocalDate baseDate;

    @Schema(description = "交易日偏移量", example = "2")
    private Integer offset;

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "查询到的交易日", example = "2023-06-20")
    private LocalDate tradingDate;
}
