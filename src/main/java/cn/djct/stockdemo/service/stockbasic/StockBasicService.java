package cn.djct.stockdemo.service.stockbasic;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票基础信息服务。
 */
public interface StockBasicService {
    /**
     * 根据交易日，判断是否已经同步。
     * @param tradeDate 交易日
     * @return 是否已经同步
     */
    boolean hasSynchronized(LocalDate tradeDate);

    /**
     * 根据最新股票代码，查询股票快照数量。
     * @return 股票快照数量
     */
    int countLatestSnapshot();

    /**
     * 根据交易日，查询股票快照数量。
      * @param tradeDate 交易日
     * @return 股票快照数量
     */
    int countSnapshot(LocalDate tradeDate);

    /**
     * 根据交易日、最后股票代码和限制数量，查询股票快照批次。
     * @param tradeDate 交易日
     * @param lastStockCode 最后股票代码
     * @param limit 限制数量
     * @return 股票快照批次列表
     */
    List<StockBasic> findSnapshotBatch(LocalDate tradeDate, String lastStockCode, int limit);

    /**
     * 保存股票快照。
      * @param tradeDate 交易日
      * @param stocks 股票快照列表
      * @return 影响行数
     */
    int saveSnapshot(LocalDate tradeDate, List<StockBasicDto> stocks);
}
