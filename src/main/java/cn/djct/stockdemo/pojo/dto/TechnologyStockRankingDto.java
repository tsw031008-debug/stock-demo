package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 热门和潜力科技股榜单业务数据。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockRankingDto {

    private LocalDate statisticsDate;
    private List<TechnologyStockRankDto> hotStocks;
    private List<TechnologyStockRankDto> potentialStocks;
}
