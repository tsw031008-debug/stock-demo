package cn.djct.stockdemo.service.stockdailyquote;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情外部数据源。
 */
public interface StockDailyQuoteSourceService {

    /**
     * 获取所有股票的日行情。
     * @param tradeDate 交易日期
     * @param stocks 股票
     * @return  股票日行情
     */
    List<StockDailyQuote> fetchAll(LocalDate tradeDate, List<StockBasic> stocks);

    /**
     * 根据指定股票代码批量获取日行情，不扩展为全市场查询。
     *
     * @param tradeDate 交易日期
     * @param stockCodes 股票代码
     * @return 指定股票的日行情
     */
    List<StockDailyQuote> fetchByCodes(LocalDate tradeDate, List<String> stockCodes);
}
