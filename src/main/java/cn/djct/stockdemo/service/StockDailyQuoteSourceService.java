package cn.djct.stockdemo.service;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情外部数据源。
 */
public interface StockDailyQuoteSourceService {

    List<StockDailyQuote> fetchAll(LocalDate tradeDate, List<StockBasic> stocks);
}
