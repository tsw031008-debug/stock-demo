package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 市场周月平均成交额同比环比响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPeriodComparisonRespVo {

    @JSONField(format = "yyyy-MM-dd")
    private LocalDate statisticsTradeDate;
    private List<MarketPeriodItemRespVo> weekly;
    private List<MarketPeriodItemRespVo> monthly;
}
