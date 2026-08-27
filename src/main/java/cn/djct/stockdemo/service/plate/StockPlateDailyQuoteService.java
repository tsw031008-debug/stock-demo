package cn.djct.stockdemo.service.plate;

import java.time.LocalDate;

/**
 * 股票板块日线服务。
 */
public interface StockPlateDailyQuoteService {

    /**
     * 计算并保存指定交易日的板块日线。
     *
     * @param tradeDate 交易日
     * @return 保存的板块日线数量
     */
    int synchronize(LocalDate tradeDate);

    /**
     * 使用当前板块成分股回补指定年份年初至结束日期的板块日线。
     *
     * @param endDate 结束交易日
     * @return 处理的板块日线数量
     */
    int backfillCurrentYear(LocalDate endDate);
}
