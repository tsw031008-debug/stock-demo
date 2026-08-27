package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票板块实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlate {

    private Long id;
    private String plateName;
    private String dataSource;
    private LocalDate lastSeenTradeDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
