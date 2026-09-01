package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 交易日判断响应。
 */
@Data
@Builder
@Schema(description = "交易日判断响应")
public class TradingDayRespVo {

    @JSONField(format = "yyyy-MM-dd")
    @Schema(description = "查询日期", example = "2023-06-26")
    private LocalDate date;

    @Schema(description = "是否为交易日", example = "true")
    private Boolean tradingDay;
}
