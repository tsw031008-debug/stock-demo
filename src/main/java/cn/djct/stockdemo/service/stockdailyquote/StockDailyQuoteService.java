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

    /** 检查指定股票批次是否已保存同日盘后完整行情，缺行或盘中记录返回false。 */
    boolean hasClosingQuotes(LocalDate tradeDate, List<String> stockCodes);

    /**
     * 保存行情快照数据。
     * @param quotes
     * @return
     */
    // 保存行情快照数据
    int saveSnapshot(List<StockDailyQuote> quotes);
}
