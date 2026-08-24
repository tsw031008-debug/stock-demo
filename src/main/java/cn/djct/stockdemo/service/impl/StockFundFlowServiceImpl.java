package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.StockFundFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 股票资金流向持久化服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockFundFlowServiceImpl implements StockFundFlowService {

    private static final int SAVE_BATCH_SIZE = 500;

    private final StockFundFlowMapper stockFundFlowMapper;

    @Override
    public int countByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockFundFlowMapper.countByTradeDate(tradeDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSnapshot(List<StockFundFlow> fundFlows) {
        Objects.requireNonNull(fundFlows, "股票资金流向不能为空");
        if (fundFlows.isEmpty()) {
            throw new IllegalArgumentException("股票资金流向不能为空");
        }
        LocalDate tradeDate = fundFlows.get(0).getTradeDate();
        if (fundFlows.stream().anyMatch(fundFlow -> !tradeDate.equals(fundFlow.getTradeDate()))) {
            throw new IllegalArgumentException("股票资金流向必须属于同一交易日");
        }

        for (int startIndex = 0; startIndex < fundFlows.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, fundFlows.size());
            stockFundFlowMapper.upsertBatch(
                    new ArrayList<>(fundFlows.subList(startIndex, endIndex))
            );
        }
        int savedCount = stockFundFlowMapper.countByTradeDate(tradeDate);
        if (savedCount != fundFlows.size()) {
            throw new IllegalStateException("股票资金流向落库数量不一致，expected="
                    + fundFlows.size() + "，actual=" + savedCount);
        }
        return fundFlows.size();
    }
}
