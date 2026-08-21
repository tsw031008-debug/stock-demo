package cn.djct.stockdemo.service.impl;

import cn.djct.stockdemo.face.StockBasicCollectionFace;
import cn.djct.stockdemo.service.StockBasicSyncService;
import cn.djct.stockdemo.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 股票基础信息同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockBasicSyncServiceImpl implements StockBasicSyncService {

    private final TradeCalendarService tradeCalendarService;

    private final StockBasicCollectionFace stockBasicCollectionFace;

    @Override
    public int synchronize(LocalDate tradeDate) {
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，股票基础信息同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        return stockBasicCollectionFace.synchronize(tradeDate);
    }
}
