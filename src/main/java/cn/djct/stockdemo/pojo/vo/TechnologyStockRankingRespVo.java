package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 热门和潜力科技股榜单响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockRankingRespVo {

    @JSONField(format = "yyyy-MM-dd")
    private LocalDate statisticsDate;
    private List<TechnologyStockRankRespVo> hotStocks;
    private List<TechnologyStockRankRespVo> potentialStocks;
}
