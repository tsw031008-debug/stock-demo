package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSourceService;
import cn.djct.stockdemo.service.indexstyle.IndexEtfDailyQuoteSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 四只固定指数ETF日行情同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexEtfDailyQuoteSyncServiceImpl implements IndexEtfDailyQuoteSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final IndexEtfDailyQuoteSourceService sourceService;
    private final IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    /**
     * 已有完整数据时不重复请求腾讯；残缺数据通过整批行情幂等修复。
     */
    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，指数ETF日行情同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        List<String> etfCodes = Arrays.stream(IndexEtf.values())
                .map(IndexEtf::getEtfCode)
                .toList();
        List<IndexEtfDailyQuote> existingQuotes = indexEtfDailyQuoteMapper
                .selectByTradeDatesAndCodes(List.of(tradeDate), etfCodes);
        if (hasAllCodes(tradeDate, existingQuotes)) {
            log.info("指数ETF日行情当天已经完整，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }

        List<IndexEtfDailyQuote> quotes = sourceService.fetch(tradeDate);
        if (!hasAllCodes(tradeDate, quotes)) {
            throw new IllegalStateException("指数ETF日行情不完整，tradeDate=" + tradeDate);
        }
        indexEtfDailyQuoteMapper.upsertBatch(quotes);
        return quotes.size();
    }

    private boolean hasAllCodes(LocalDate tradeDate, List<IndexEtfDailyQuote> quotes) {
        if (quotes == null || quotes.size() != IndexEtf.values().length) {
            return false;
        }
        Set<String> actualCodes = new HashSet<>();
        for (IndexEtfDailyQuote quote : quotes) {
            if (quote == null || !tradeDate.equals(quote.getTradeDate())
                    || !actualCodes.add(quote.getEtfCode())) {
                return false;
            }
        }
        return Arrays.stream(IndexEtf.values())
                .map(IndexEtf::getEtfCode)
                .allMatch(actualCodes::contains);
    }
}
