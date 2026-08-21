package cn.djct.stockdemo.service;

import java.time.LocalDate;

/**
 * 股票日行情同步服务。
 */
public interface StockDailyQuoteSyncService {

    int synchronize(LocalDate tradeDate);
}
