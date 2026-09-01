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

    /**
     * 根据最后 seen 交易日期统计股票数量。
      * @param tradeDate 交易日期
      * @return 股票数量
     */
    int countByLastSeenTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 统计最新股票快照数量。
      * @return 股票快照数量
     */
    int countLatestSnapshot();

    /**
     * 根据交易日期、股票代码和批次大小，分批获取股票快照。
      * @param tradeDate 交易日期
      * @param lastStockCode 上一股票代码
      * @param limit 批次大小
      * @return 股票快照
     */
    List<StockBasic> selectSnapshotAfterCode(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("lastStockCode") String lastStockCode,
            @Param("limit") int limit
    );

    /**
     * 批量插入或更新股票快照。
      * @param stocks 股票快照列表
      * @return 影响行数
     */
    int upsertBatch(@Param("list") List<StockBasic> stocks);
}
