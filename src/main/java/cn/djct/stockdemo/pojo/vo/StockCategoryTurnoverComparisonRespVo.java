package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 四大类两交易日成交额对比响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCategoryTurnoverComparisonRespVo {
    @Schema(description = "当前交易日")
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate statisticsDate;
    @Schema(description = "上一交易日")
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate previousTradeDate;
    private List<StockCategoryTurnoverItemRespVo> categories;
}
