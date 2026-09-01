package cn.djct.stockdemo.service.plate;

import java.time.LocalDate;

/**
 * 股票板块同步服务。
 */
public interface StockPlateSyncService {

    /**
     * 同步指定交易日的板块和成分股快照。
     *
     * @param tradeDate 交易日
     * @return 保存的板块数量
     */
    int synchronize(LocalDate tradeDate);
}
