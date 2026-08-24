package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票资金流向数据访问接口。
 */
@Mapper
public interface StockFundFlowMapper {

    int countByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    int upsertBatch(@Param("list") List<StockFundFlow> fundFlows);
}
