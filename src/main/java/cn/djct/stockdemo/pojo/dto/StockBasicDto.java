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

    private String stockCode;

    private String stockName;
}
