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

    private Long id;

    private String stockCode;

    private String stockName;

    private LocalDate lastSeenTradeDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
