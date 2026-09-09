package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 近期股票涨幅家数响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentStockRiseCountRespVo {

    @JSONField(format = "yyyy-MM-dd")
    private LocalDate tradeDate;

    private Integer fiveDayRiseCount;

    private Integer tenDayRiseCount;
}
