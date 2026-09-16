package cn.djct.stockdemo.pojo.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 指定交易日个股相对沪深300的差异分布。 */
@Data
@Builder
public class StockIndexDifferenceRespVo {
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate tradeDate;
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate previousTradeDate;
    private String indexCode;
    @Schema(description = "查询日已落库的股票数量，不代表全市场数据完整性")
    private int totalStockCount;
    private int validStockCount;
    @Schema(description = "查询日股票中，任一天收盘价缺失或不大于0的数量")
    private int skippedStockCount;
    @Schema(description = "强势累计统计，按1、3、5、7个百分点排列")
    private List<StockIndexDifferenceItemVo> strong;
    @Schema(description = "弱势累计统计，按-1、-3、-5、-7个百分点排列")
    private List<StockIndexDifferenceItemVo> weak;
}
