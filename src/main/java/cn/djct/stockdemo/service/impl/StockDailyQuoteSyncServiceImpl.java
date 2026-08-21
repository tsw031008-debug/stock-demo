package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.face.StockDailyQuoteCollectionFace;
import cn.djct.stockdemo.service.StockDailyQuoteSyncService;
import cn.djct.stockdemo.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 股票日行情同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDailyQuoteSyncServiceImpl implements StockDailyQuoteSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace;

    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，股票日行情同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        return stockDailyQuoteCollectionFace.synchronize(tradeDate);
    }
}
