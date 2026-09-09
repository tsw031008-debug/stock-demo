package cn.djct.stockdemo.service.indexstyle;

import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 四只固定指数ETF日行情外部数据源。
 */
public interface IndexEtfDailyQuoteSourceService {

    /**
     * 获取指定交易日的四只固定ETF收盘行情。
     *
     * @param tradeDate 预期交易日
     * @return 四只ETF日行情
     */
    List<IndexEtfDailyQuote> fetch(LocalDate tradeDate);
}
