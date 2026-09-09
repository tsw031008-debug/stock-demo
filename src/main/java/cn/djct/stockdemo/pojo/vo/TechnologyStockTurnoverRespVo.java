package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.fastjson2.annotation.JSONField;

import java.time.LocalDate;
import java.util.List;

/** 指定交易日的科技股成交额异动三类榜单。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyStockTurnoverRespVo {
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate statisticsDate;
    private List<TechnologyStockRankRespVo> increasingStocks;
    private List<TechnologyStockRankRespVo> institutionalStocks;
    private List<TechnologyStockRankRespVo> abnormalStocks;
}
