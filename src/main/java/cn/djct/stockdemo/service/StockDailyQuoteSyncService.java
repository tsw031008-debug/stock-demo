package cn.djct.stockdemo.service;

import java.time.LocalDate;

/**
 * 股票日行情同步服务。
 */
public interface StockDailyQuoteSyncService {

    /**
     * 同步指定交易日的股票日行情数据。
     *
     * @param tradeDate 交易日
     * @return 保存的记录数
     */
    int synchronize(LocalDate tradeDate);
}
