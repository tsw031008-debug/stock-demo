package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科技股榜单明细响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockRankRespVo {

    private Integer rank;
    private String stockCode;
    private String stockName;
}
