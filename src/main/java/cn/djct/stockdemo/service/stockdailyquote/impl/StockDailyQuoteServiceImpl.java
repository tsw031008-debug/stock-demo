package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;

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

    @Override
    public boolean hasClosingQuotes(LocalDate tradeDate, List<String> stockCodes) {
        if (stockCodes.isEmpty()) {
            return false;
        }
        List<StockDailyQuote> quotes = stockDailyQuoteMapper.selectByStockCodesAndTradeDates(
                stockCodes, List.of(tradeDate));
        return quotes.size() == stockCodes.size()
                && new HashSet<>(quotes.stream().map(StockDailyQuote::getStockCode).toList())
                    .equals(new HashSet<>(stockCodes))
                && quotes.stream().allMatch(quote -> isClosingQuote(quote, tradeDate));
    }

    private boolean isClosingQuote(StockDailyQuote quote, LocalDate tradeDate) {
        if (quote == null || !tradeDate.equals(quote.getTradeDate())
                || !"TENCENT".equals(quote.getDataSource())
                || quote.getClosePrice() == null || quote.getTurnoverAmountYuan() == null
                || quote.getClosePrice().signum() < 0 || quote.getTurnoverAmountYuan().signum() < 0) {
            return false;
        }
        LocalDateTime closeTime = tradeDate.atTime(15, 0);
        // 沿用项目停牌口径：价格及成交额同时为0；只接受当天盘后实际采到的停牌记录。
        if (quote.getClosePrice().signum() == 0 && quote.getTurnoverAmountYuan().signum() == 0) {
            return quote.getCollectedAt() != null && !quote.getCollectedAt().isBefore(closeTime)
                    && tradeDate.equals(quote.getCollectedAt().toLocalDate());
        }
        return "COMPLETE".equals(quote.getDataStatus()) && quote.getQuoteTime() != null
                && tradeDate.equals(quote.getQuoteTime().toLocalDate())
                && !quote.getQuoteTime().isBefore(closeTime)
                && quote.getOpenPrice() != null && quote.getHighPrice() != null
                && quote.getLowPrice() != null && quote.getPreviousClosePrice() != null
                && quote.getChangePercent() != null;
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
        if (quotes.stream().anyMatch(quote -> !isClosingQuote(quote, tradeDate))) {
            throw new IllegalStateException("日行情尚未形成完整收盘快照，禁止写入盘中或过期行情");
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
