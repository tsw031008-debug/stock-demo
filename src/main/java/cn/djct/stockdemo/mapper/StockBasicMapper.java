package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票基础信息数据访问接口。
 */
@Mapper
public interface StockBasicMapper {

    int countByLastSeenTradeDate(@Param("tradeDate") LocalDate tradeDate);

    int countLatestSnapshot();

    List<StockBasic> selectSnapshotAfterCode(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("lastStockCode") String lastStockCode,
            @Param("limit") int limit
    );

    int upsertBatch(@Param("list") List<StockBasic> stocks);
}
