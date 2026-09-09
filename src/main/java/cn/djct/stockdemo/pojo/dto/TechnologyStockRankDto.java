package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科技股榜单明细。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockRankDto {

    private Integer rank;
    private String stockCode;
    private String stockName;
}
