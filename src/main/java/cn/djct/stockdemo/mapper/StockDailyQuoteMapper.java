package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.dto.StockTurnoverByDateDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情数据访问接口。
 */
@Mapper
public interface StockDailyQuoteMapper {

    /**
     * 查询股票日行情最新交易日。
     *
     * @return 最新交易日；没有数据时返回null
     */
    LocalDate selectLatestTradeDate();

    /**
     * 根据交易日查询行情数据条数。
     * @param tradeDate 交易日
     * @return  行情数据条数
     */
    int countByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询指定交易日的沪深A股成交额原始记录。
     *
     * @param tradeDates 交易日列表
     * @return 成交额原始记录
     */
    List<MarketTurnoverRecordDto> selectMarketTurnoverRecords(
            @Param("tradeDates") List<LocalDate> tradeDates
    );

    /**
     * 查询指定交易日的股票历史收盘价原始数据。
     *
     * @param tradeDates 交易日列表
     * @return 股票历史收盘价
     */
    List<StockClosePriceDto> selectClosePricesByTradeDates(
            @Param("tradeDates") List<LocalDate> tradeDates
    );

    /**
     * 查询指定交易日用于板块计算的股票行情原始数据。
     *
     * @param tradeDate 交易日
     * @return 股票行情原始数据
     */
    List<StockDailyQuote> selectForPlateCalculation(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询指定交易日的股票成交额原始记录。
     *
     * @param tradeDates 交易日列表
     * @param stockCodes 参与四大类计算的股票代码
     * @return 按股票和日期区分的成交额
     */
    List<StockTurnoverByDateDto> selectTurnoversByTradeDates(
            @Param("tradeDates") List<LocalDate> tradeDates,
            @Param("stockCodes") List<String> stockCodes
    );

    /**
     * 批量插入或更新行情数据。
     * @param quotes 行情数据列表
     * @return  插入或更新的行数
     */
    int upsertBatch(@Param("list") List<StockDailyQuote> quotes);

    /** 按代码批次和明确的交易日列表读取原始行情，按代码、日期升序返回。 */
    List<StockDailyQuote> selectByStockCodesAndTradeDates(
            @Param("stockCodes") List<String> stockCodes,
            @Param("tradeDates") List<LocalDate> tradeDates
    );

    /** 定位指定股票已有历史的最早交易日，用于区分窗口前端缺失与历史不足。 */
    LocalDate selectFirstQuoteDate(@Param("stockCode") String stockCode);
}
