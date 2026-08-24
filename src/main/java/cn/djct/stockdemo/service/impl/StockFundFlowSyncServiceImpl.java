package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.face.StockFundFlowCollectionFace;
import cn.djct.stockdemo.service.StockFundFlowSyncService;
import cn.djct.stockdemo.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 股票资金流向同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundFlowSyncServiceImpl implements StockFundFlowSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final StockFundFlowCollectionFace stockFundFlowCollectionFace;

    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，股票资金流向同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        return stockFundFlowCollectionFace.synchronize(tradeDate);
    }
}
