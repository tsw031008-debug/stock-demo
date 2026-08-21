package cn.djct.stockdemo.service;

import java.time.LocalDate;

/**
 * 股票基础信息同步服务。
 */
public interface StockBasicSyncService {

    /**
     * 同步股票基础信息。
     * @param tradeDate 交易日期
     * @return 保存的股票数量
     */
    int synchronize(LocalDate tradeDate);
}
