package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.service.StockBasicService;
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

    @Override
    public boolean hasSynchronized(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockBasicMapper.countByLastSeenTradeDate(tradeDate) > 0;
    }

    @Override
    public int countLatestSnapshot() {
        return stockBasicMapper.countLatestSnapshot();
    }

    @Override
    public int countSnapshot(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockBasicMapper.countByLastSeenTradeDate(tradeDate);
    }

    @Override
    public List<StockBasic> findSnapshotBatch(LocalDate tradeDate, String lastStockCode, int limit) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Objects.requireNonNull(lastStockCode, "上一股票代码不能为空");
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("股票清单批次大小必须在1到1000之间");
        }
        return stockBasicMapper.selectSnapshotAfterCode(tradeDate, lastStockCode, limit);
    }

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
