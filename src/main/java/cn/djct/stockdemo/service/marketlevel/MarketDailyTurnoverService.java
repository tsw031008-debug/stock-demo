package cn.djct.stockdemo.service.marketlevel;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日市场成交额同步服务。
 */
public interface MarketDailyTurnoverService {

    int synchronize(LocalDate tradeDate);

    /** 查询最多31个自然日内缺少完整汇总的交易日，供漏跑检查使用，不自动写入历史数据。 */
    List<LocalDate> findMissingTradeDates(LocalDate startDate, LocalDate endDate);
}
