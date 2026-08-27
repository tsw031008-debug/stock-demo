package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 板块收盘值原始数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateCloseDto {

    private Long plateId;
    private BigDecimal closePrice;
}
