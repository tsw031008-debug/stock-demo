package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 股票基础信息传输对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockBasicDto {

    // 股票代码
    private String stockCode;

    // 股票名称
    private String stockName;
}
