package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
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
     * 批量插入或更新行情数据。
     * @param quotes 行情数据列表
     * @return  插入或更新的行数
     */
    int upsertBatch(@Param("list") List<StockDailyQuote> quotes);
}
