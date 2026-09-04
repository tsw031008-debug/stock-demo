package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockPlateCloseDto;
import cn.djct.stockdemo.pojo.entity.StockPlateDailyQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票板块日线数据访问接口。
 */
@Mapper
public interface StockPlateDailyQuoteMapper {

    /**
     * 定位最近一个已完成全量板块日线计算的交易日。
     *
     * @return 最近完整交易日，无数据时返回null
     */
    LocalDate selectLatestCompleteTradeDate();

    /**
     * 批量插入或更新板块日线。
     *
     * @param quotes 板块日线列表
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<StockPlateDailyQuote> quotes);

    /**
     * 统计指定交易日的板块日线数量。
     *
     * @param tradeDate 交易日
     * @return 日线数量
     */
    int countByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 统计指定交易日的完整板块日线数量。
     *
     * @param tradeDate 交易日
     * @return 完整日线数量
     */
    int countCompleteByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询指定交易日的板块收盘值。
     *
     * @param tradeDate 交易日
     * @return 板块收盘值列表
     */
    List<StockPlateCloseDto> selectCloseByTradeDate(@Param("tradeDate") LocalDate tradeDate);
}
