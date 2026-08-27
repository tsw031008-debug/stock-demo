package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
import cn.djct.stockdemo.service.plate.StockPlateService;
import cn.djct.stockdemo.service.plate.StockPlateSourceService;
import cn.djct.stockdemo.service.plate.StockPlateSyncService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 股票板块同步服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPlateSyncServiceImpl implements StockPlateSyncService {

    private final TradeCalendarService tradeCalendarService;
    private final StockPlateSourceService stockPlateSourceService;
    private final StockPlateService stockPlateService;

    /**
     * 同步指定交易日的完整板块和成分股快照。
     *
     * @param tradeDate 交易日
     * @return 保存的板块数量
     */
    @Override
    public int synchronize(LocalDate tradeDate) {
        // 非交易日和当天已同步时均不访问板块来源接口
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            log.info("当天不是交易日，板块同步跳过，tradeDate={}", tradeDate);
            return 0;
        }
        if (stockPlateService.hasSynchronized(tradeDate)) {
            log.info("当天板块已经同步，本次触发跳过，tradeDate={}", tradeDate);
            return 0;
        }

        // 外部请求在事务之外完成，完整解析成功后再进入持久化事务
        List<StockPlateSourceDto> plates = stockPlateSourceService.fetchAll();
        int savedCount = stockPlateService.saveSnapshot(tradeDate, plates);
        log.info("板块同步完成，tradeDate={}，savedCount={}", tradeDate, savedCount);
        return savedCount;
    }
}
