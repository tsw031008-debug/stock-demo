package cn.djct.stockdemo.service.indexstyle;

import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数日行情外部数据源。
 */
public interface IndexDailyQuoteSourceService {

    /**
     * 获取四个固定指数在指定交易日的收盘行情。
     *
     * @param tradeDate 预期交易日
     * @return 四个指数的日行情
     */
    List<IndexDailyQuote> fetch(LocalDate tradeDate);
}
