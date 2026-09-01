package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数分钟MACD计算结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMacdDto {

    private LocalDateTime quoteTime;
    private BigDecimal currentPrice;
    private BigDecimal dif;
    private BigDecimal dea;
    private BigDecimal macd;
}
