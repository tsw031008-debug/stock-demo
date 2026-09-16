package cn.djct.stockdemo.service.indexstyle;

import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceRespVo;
import java.time.LocalDate;

/** 个股与沪深300差异分布查询。 */
public interface StockIndexDifferenceService {
    /** 按指定交易日动态计算，日期或必要行情不可用时提示错误。 */
    StockIndexDifferenceRespVo findByTradeDate(LocalDate tradeDate);
}
