package cn.djct.stockdemo.service.stockfundflow.impl;

import cn.djct.stockdemo.face.StockFundFlowCollectionFace;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSyncService;
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
 * 股票资金流向同步服务实现。
 */
@Slf4j
@Service
public class StockFundFlowSyncServiceImpl implements StockFundFlowSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final StockFundFlowCollectionFace stockFundFlowCollectionFace;
    private final Clock clock;

    @Autowired
    public StockFundFlowSyncServiceImpl(TradeCalendarService tradeCalendarService,
            StockFundFlowCollectionFace stockFundFlowCollectionFace) {
        this(tradeCalendarService, stockFundFlowCollectionFace, Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    public StockFundFlowSyncServiceImpl(TradeCalendarService tradeCalendarService,
            StockFundFlowCollectionFace stockFundFlowCollectionFace, Clock clock) {
        this.tradeCalendarService = tradeCalendarService;
        this.stockFundFlowCollectionFace = stockFundFlowCollectionFace;
        this.clock = clock;
    }


    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，股票资金流向同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        // 实时数据源只能生成当天盘后记录，禁止手动入口写入盘中或伪造历史日线。
        if (!tradeDate.equals(now.toLocalDate()) || now.toLocalTime().isBefore(LocalTime.of(15, 20))) {
            throw new IllegalStateException("仅允许当天15:20及之后同步收盘数据");
        }
        return stockFundFlowCollectionFace.synchronize(tradeDate);
    }
}
