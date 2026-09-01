package cn.djct.stockdemo.service.stockdailyquote;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情持久化服务。
 */
public interface StockDailyQuoteService {

    /**
     * 根据交易日查询行情数据条数。
     * @param tradeDate
     * @return
     */
    int countByTradeDate(LocalDate tradeDate);

    /**
     * 保存行情快照数据。
     * @param quotes
     * @return
     */
    // 保存行情快照数据
    int saveSnapshot(List<StockDailyQuote> quotes);
}
