package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSourceService;
import cn.djct.stockdemo.service.indexstyle.IndexDailyQuoteSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 指数日行情同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexDailyQuoteSyncServiceImpl implements IndexDailyQuoteSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final IndexDailyQuoteSourceService sourceService;
    private final IndexDailyQuoteMapper indexDailyQuoteMapper;

    /**
     * 交易日收盘后同步四个固定指数，任一指数缺失时整批拒绝保存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，指数日行情同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        List<IndexDailyQuote> quotes = sourceService.fetch(tradeDate);
        validateComplete(tradeDate, quotes);
        indexDailyQuoteMapper.upsertBatch(quotes);
        return quotes.size();
    }

    private void validateComplete(LocalDate tradeDate, List<IndexDailyQuote> quotes) {
        if (quotes == null || quotes.size() != IndexStyleIndex.values().length) {
            throw new IllegalStateException("指数日行情数量不完整，tradeDate=" + tradeDate);
        }
        Set<String> actualCodes = new HashSet<>();
        for (IndexDailyQuote quote : quotes) {
            if (!tradeDate.equals(quote.getTradeDate())) {
                throw new IllegalStateException("指数日行情日期不一致，indexCode="
                        + quote.getIndexCode());
            }
            if (!actualCodes.add(quote.getIndexCode())) {
                throw new IllegalStateException("指数日行情代码重复，indexCode="
                        + quote.getIndexCode());
            }
        }
        for (IndexStyleIndex index : IndexStyleIndex.values()) {
            if (!actualCodes.contains(index.getIndexCode())) {
                throw new IllegalStateException("指数日行情缺失，indexCode=" + index.getIndexCode());
            }
        }
    }
}
