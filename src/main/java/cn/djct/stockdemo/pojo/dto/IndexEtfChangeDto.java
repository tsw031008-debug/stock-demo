package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单只指数ETF的5日涨幅。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexEtfChangeDto {

    private String etfCode;
    private String indexName;
    private BigDecimal changePercent;
}
