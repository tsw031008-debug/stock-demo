package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票板块成分股实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateMember {

    private Long id;
    private Long plateId;
    private String stockCode;
    private LocalDate lastSeenTradeDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
