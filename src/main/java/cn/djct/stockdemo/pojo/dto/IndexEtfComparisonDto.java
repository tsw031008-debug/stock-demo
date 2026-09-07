package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 四只固定指数ETF的5日涨幅比较结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexEtfComparisonDto {

    private LocalDate statisticsDate;
    private LocalDate baseTradeDate;
    private List<IndexEtfChangeDto> etfs;
}
