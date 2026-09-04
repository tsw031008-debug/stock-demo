package cn.djct.stockdemo.pojo.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 交易日历初始化请求参数。
 */
@Data
@Schema(description = "交易日历初始化请求")
public class TradeCalendarInitializeDto {

    @NotNull(message = "开始日期不能为空")
    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "开始日期", example = "2017-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "结束日期", example = "2026-12-31", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate endDate;

    @AssertTrue(message = "开始日期不能晚于结束日期")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
