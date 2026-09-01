package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 市场周月平均成交额同比环比结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPeriodComparisonDto {

    private LocalDate statisticsTradeDate;
    private List<MarketPeriodItemDto> weekly;
    private List<MarketPeriodItemDto> monthly;
}
