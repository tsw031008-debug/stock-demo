package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数分钟曲线点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMinuteCurvePointDto {

    private LocalDateTime quoteTime;
    private BigDecimal currentPrice;
}
