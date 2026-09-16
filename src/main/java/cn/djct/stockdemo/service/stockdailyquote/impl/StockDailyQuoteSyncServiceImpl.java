package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.face.StockDailyQuoteCollectionFace;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Clock;
import java.time.ZoneId;

/**
 * 股票日行情同步服务实现。
 */
@Slf4j
@Service
public class StockDailyQuoteSyncServiceImpl implements StockDailyQuoteSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace;
    private final Clock clock;

    @Autowired
    public StockDailyQuoteSyncServiceImpl(TradeCalendarService tradeCalendarService,
            StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace) {
        this(tradeCalendarService, stockDailyQuoteCollectionFace, Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    public StockDailyQuoteSyncServiceImpl(TradeCalendarService tradeCalendarService,
            StockDailyQuoteCollectionFace stockDailyQuoteCollectionFace, Clock clock) {
        this.tradeCalendarService = tradeCalendarService;
        this.stockDailyQuoteCollectionFace = stockDailyQuoteCollectionFace;
        this.clock = clock;
    }


    /**
     * 同步指定交易日的股票日行情数据。
     *
     * @param tradeDate 交易日
     * @return 保存的记录数
     */
    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，股票日行情同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        // 同步股票日行情数据
        LocalDateTime now = LocalDateTime.now(clock);
        // 实时数据源只能生成当天盘后记录，禁止手动入口写入盘中或伪造历史日线。
        if (!tradeDate.equals(now.toLocalDate()) || now.toLocalTime().isBefore(LocalTime.of(15, 2))) {
            throw new IllegalStateException("仅允许当天15:02及之后同步收盘数据");
        }
        return stockDailyQuoteCollectionFace.synchronize(tradeDate);
    }
}
