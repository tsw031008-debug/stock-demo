package cn.djct.stockdemo.service.indexstyle;

import java.time.LocalDate;

/**
 * 四只固定指数ETF日行情同步服务。
 */
public interface IndexEtfDailyQuoteSyncService {

    /**
     * 同步指定交易日的四只固定ETF日行情。
     *
     * @param tradeDate 交易日
     * @return 保存数量，已有完整数据或非交易日时返回0
     */
    int synchronize(LocalDate tradeDate);
}
