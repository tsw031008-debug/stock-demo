package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 近期股票涨幅家数统计结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentStockRiseCountDto {

    private LocalDate tradeDate;

    private Integer fiveDayRiseCount;

    private Integer tenDayRiseCount;
}
