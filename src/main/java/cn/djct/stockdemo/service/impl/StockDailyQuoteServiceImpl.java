package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.StockDailyQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 股票日行情持久化服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockDailyQuoteServiceImpl implements StockDailyQuoteService {

    private static final int SAVE_BATCH_SIZE = 500;

    private final StockDailyQuoteMapper stockDailyQuoteMapper;

    /**
     * 根据交易日查询行情数据条数。
      * @param tradeDate 交易日
      * @return 行情数据条数
     */
    @Override
    public int countByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockDailyQuoteMapper.countByTradeDate(tradeDate);
    }

    /**
     * 保存行情快照数据。
     * @param quotes 行情数据
     * @return  行情数据条数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSnapshot(List<StockDailyQuote> quotes) {
        Objects.requireNonNull(quotes, "股票日行情不能为空");
        if (quotes.isEmpty()) {
            throw new IllegalArgumentException("股票日行情不能为空");
        }
        // 确保所有行情数据属于同一交易日
        LocalDate tradeDate = quotes.get(0).getTradeDate();
        if (quotes.stream().anyMatch(quote -> !tradeDate.equals(quote.getTradeDate()))) {
            throw new IllegalArgumentException("股票日行情必须属于同一交易日");
        }
        // 分批保存数据
        for (int startIndex = 0; startIndex < quotes.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, quotes.size());
            stockDailyQuoteMapper.upsertBatch(new ArrayList<>(quotes.subList(startIndex, endIndex)));
        }
        // 确保保存数量一致
        int savedCount = stockDailyQuoteMapper.countByTradeDate(tradeDate);
        if (savedCount != quotes.size()) {
            throw new IllegalStateException("股票日行情落库数量不一致，expected="
                    + quotes.size() + "，actual=" + savedCount);
        }
        return quotes.size();
    }
}
