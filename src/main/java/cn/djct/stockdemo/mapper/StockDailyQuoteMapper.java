package cn.djct.stockdemo.mapper;

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

    int countByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    int upsertBatch(@Param("list") List<StockDailyQuote> quotes);
}
