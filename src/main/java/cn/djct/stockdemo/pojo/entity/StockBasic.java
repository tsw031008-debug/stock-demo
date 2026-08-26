package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票基础信息实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockBasic {

    //
    private Long id;

    // 股票代码
    private String stockCode;

    // 股票名称
    private String stockName;

    // 最后一次交易日期
    private LocalDate lastSeenTradeDate;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
