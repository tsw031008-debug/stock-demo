package cn.djct.stockdemo.service;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情外部数据源。
 */
public interface StockDailyQuoteSourceService {

    /**
     * 获取所有股票的日行情。
     * @param tradeDate 交易日期
     * @param stocks 股票
     * @return  股票日行情
     */
    List<StockDailyQuote> fetchAll(LocalDate tradeDate, List<StockBasic> stocks);
}
