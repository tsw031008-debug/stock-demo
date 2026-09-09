package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.pojo.vo.TechnologyStockTurnoverRespVo;
import java.time.LocalDate;

/** 科技股成交额异动查询。 */
public interface TechnologyStockTurnoverService {
    /** 使用当前科技成分股查询指定交易日的三类前五名，不回退日期。 */
    TechnologyStockTurnoverRespVo findByTradeDate(LocalDate tradeDate);
}
