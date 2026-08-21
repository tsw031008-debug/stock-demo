package cn.djct.stockdemo.service;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情持久化服务。
 */
public interface StockDailyQuoteService {

    int countByTradeDate(LocalDate tradeDate);

    int saveSnapshot(List<StockDailyQuote> quotes);
}
