package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 四大类当前与上一交易日成交额对比。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCategoryTurnoverComparisonDto {
    private LocalDate statisticsDate;
    private LocalDate previousTradeDate;
    private List<StockCategoryTurnoverItemDto> categories;
}
