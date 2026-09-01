package cn.djct.stockdemo.service.marketlevel;

import java.time.LocalDate;

/**
 * 每日市场成交额同步服务。
 */
public interface MarketDailyTurnoverService {

    int synchronize(LocalDate tradeDate);
}
