package cn.djct.stockdemo.service.indexstyle;

import java.time.LocalDate;

/**
 * 指数日行情同步服务。
 */
public interface IndexDailyQuoteSyncService {

    /**
     * 同步指定交易日的四个固定指数日行情。
     *
     * @param tradeDate 交易日
     * @return 保存数量
     */
    int synchronize(LocalDate tradeDate);
}
