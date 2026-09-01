package cn.djct.stockdemo.service.stockbasic.impl;

import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 股票基础信息服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockBasicServiceImpl implements StockBasicService {

    private static final int BATCH_SIZE = 500;

    private final StockBasicMapper stockBasicMapper;

    // 判断指定交易日的股票清单是否已经同步。
    @Override
    public boolean hasSynchronized(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockBasicMapper.countByLastSeenTradeDate(tradeDate) > 0;
    }

    /**
     * 统计最新股票清单数量。
      * @return 股票清单数量
     */
    @Override
    public int countLatestSnapshot() {
        return stockBasicMapper.countLatestSnapshot();
    }

    /**
     * 根据交易日期统计股票清单数量。
      * @param tradeDate 交易日期
      * @return 股票清单数量
     */
    @Override
    public int countSnapshot(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockBasicMapper.countByLastSeenTradeDate(tradeDate);
    }

    /**
     * 根据交易日期、股票代码和批次大小，分批获取股票清单。
      * @param tradeDate 交易日期
      * @param lastStockCode 上一股票代码
      * @param limit 批次大小
      * @return 股票清单
     */
    @Override
    public List<StockBasic> findSnapshotBatch(LocalDate tradeDate, String lastStockCode, int limit) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Objects.requireNonNull(lastStockCode, "上一股票代码不能为空");
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("股票清单批次大小必须在1到1000之间");
        }
        return stockBasicMapper.selectSnapshotAfterCode(tradeDate, lastStockCode, limit);
    }

    /**
     * 保存股票清单。
      * @param tradeDate 交易日期
      * @param stocks 股票清单
      * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSnapshot(LocalDate tradeDate, List<StockBasicDto> stocks) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Objects.requireNonNull(stocks, "股票清单不能为空");
        if (stocks.isEmpty()) {
            throw new IllegalArgumentException("股票清单不能为空");
        }

        List<StockBasic> entities = stocks.stream()
                .map(stock -> StockBasic.builder()
                        .stockCode(stock.getStockCode())
                        .stockName(stock.getStockName())
                        .lastSeenTradeDate(tradeDate)
                        .build())
                .toList();

        for (int startIndex = 0; startIndex < entities.size(); startIndex += BATCH_SIZE) {
            int endIndex = Math.min(startIndex + BATCH_SIZE, entities.size());
            stockBasicMapper.upsertBatch(new ArrayList<>(entities.subList(startIndex, endIndex)));
        }
        return entities.size();
    }
}
