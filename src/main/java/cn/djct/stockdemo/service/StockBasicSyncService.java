package cn.djct.stockdemo.service;

import java.time.LocalDate;

/**
 * 股票基础信息同步服务。
 */
public interface StockBasicSyncService {

    int synchronize(LocalDate tradeDate);
}
