package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 交易日历初始化响应。
 */
@Data
@Builder
@Schema(description = "交易日历初始化响应")
public class TradeCalendarInitializeRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "开始日期", example = "2017-01-01")
    private LocalDate startDate;

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "结束日期", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "新增数量", example = "3652")
    private Integer insertedCount;
}
