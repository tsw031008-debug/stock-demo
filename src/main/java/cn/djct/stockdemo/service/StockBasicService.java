package cn.djct.stockdemo.service;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票基础信息服务。
 */
public interface StockBasicService {

    boolean hasSynchronized(LocalDate tradeDate);

    int countLatestSnapshot();

    int countSnapshot(LocalDate tradeDate);

    List<StockBasic> findSnapshotBatch(LocalDate tradeDate, String lastStockCode, int limit);

    int saveSnapshot(LocalDate tradeDate, List<StockBasicDto> stocks);
}
