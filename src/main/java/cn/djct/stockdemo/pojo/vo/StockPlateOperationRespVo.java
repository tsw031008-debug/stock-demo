package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 板块同步或回补操作响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateOperationRespVo {

    private LocalDate tradeDate;
    private Integer savedCount;
}
