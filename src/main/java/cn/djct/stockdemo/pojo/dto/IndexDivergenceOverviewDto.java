package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 指数曲线和背离信号查询结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDivergenceOverviewDto {

    private LocalDate tradeDate;
    private BigDecimal previousClosePrice;
    private List<IndexMinuteCurvePointDto> curveData;
    private List<IndexDivergenceSignalDto> signalData;
}
