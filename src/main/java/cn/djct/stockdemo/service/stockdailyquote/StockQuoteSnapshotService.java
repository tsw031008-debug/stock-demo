package cn.djct.stockdemo.service.stockdailyquote;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 全市场实时行情快照服务。
 */
public interface StockQuoteSnapshotService {

    /**
     * 获取指定交易日的60秒全市场实时行情快照。
     *
     * @param tradeDate 交易日
     * @return 全市场实时行情
     */
    List<StockDailyQuote> getSnapshot(LocalDate tradeDate);
}
