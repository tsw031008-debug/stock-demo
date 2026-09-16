package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketDailyTurnoverCalculator;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDailyTurnoverServiceImplTest {

    @Mock
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private StockBasicService stockBasicService;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private StockDailyQuoteService stockDailyQuoteService;

    @Test
    void shouldAggregateAndSaveCompleteTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        MarketDailyTurnoverServiceImpl service = new MarketDailyTurnoverServiceImpl(
                marketDailyTurnoverMapper,
                stockDailyQuoteMapper,
                stockBasicService,
                tradeCalendarService,
                new MarketDailyTurnoverCalculator(), stockDailyQuoteService
        );
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(stockBasicService.countSnapshot(tradeDate)).thenReturn(2);
        when(stockDailyQuoteMapper.countByTradeDate(tradeDate)).thenReturn(2);
        when(stockBasicService.findSnapshotBatch(tradeDate, "", 1000)).thenReturn(List.of(
                StockBasic.builder().stockCode("000001").build(),
                StockBasic.builder().stockCode("600000").build()));
        when(stockDailyQuoteService.hasClosingQuotes(tradeDate, List.of("000001", "600000")))
                .thenReturn(true);
        when(stockDailyQuoteMapper.selectMarketTurnoverRecords(List.of(tradeDate))).thenReturn(List.of(
                record(tradeDate, "100.00"), record(tradeDate, "200.00")
        ));
        when(marketDailyTurnoverMapper.upsert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        assertEquals(1, service.synchronize(tradeDate));

        ArgumentCaptor<MarketDailyTurnover> captor = ArgumentCaptor.forClass(MarketDailyTurnover.class);
        verify(marketDailyTurnoverMapper).upsert(captor.capture());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getTurnoverAmountYuan());
        assertEquals("COMPLETE", captor.getValue().getDataStatus());
    }

    @Test
    void shouldProtectExistingCompleteRecord() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        MarketDailyTurnoverServiceImpl service = new MarketDailyTurnoverServiceImpl(
                marketDailyTurnoverMapper,
                stockDailyQuoteMapper,
                stockBasicService,
                tradeCalendarService,
                new MarketDailyTurnoverCalculator(), stockDailyQuoteService
        );
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(marketDailyTurnoverMapper.selectByTradeDate(tradeDate)).thenReturn(
                MarketDailyTurnover.builder().dataStatus("COMPLETE").build()
        );

        assertEquals(0, service.synchronize(tradeDate));

        verify(stockDailyQuoteMapper, never()).selectMarketTurnoverRecords(List.of(tradeDate));
    }

    private MarketTurnoverRecordDto record(LocalDate tradeDate, String amount) {
        return MarketTurnoverRecordDto.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(new BigDecimal(amount))
                .build();
    }

    @Test
    void shouldFindHistoricalGapsAcrossWeekend() {
        LocalDate start = LocalDate.of(2026, 8, 28);
        LocalDate end = LocalDate.of(2026, 9, 1);
        MarketDailyTurnoverServiceImpl service = new MarketDailyTurnoverServiceImpl(
                marketDailyTurnoverMapper, stockDailyQuoteMapper, stockBasicService,
                tradeCalendarService, new MarketDailyTurnoverCalculator(), stockDailyQuoteService);
        when(tradeCalendarService.getTradingDays(start, end))
                .thenReturn(List.of(start, LocalDate.of(2026, 8, 31), end));
        when(marketDailyTurnoverMapper.selectCompleteByDateRange(start, end)).thenReturn(List.of(
                MarketDailyTurnover.builder().tradeDate(start).build(),
                MarketDailyTurnover.builder().tradeDate(end).build()));
        assertEquals(List.of(LocalDate.of(2026, 8, 31)), service.findMissingTradeDates(start, end));
        assertThrows(IllegalArgumentException.class, () -> service.findMissingTradeDates(start.minusDays(40), end));
    }

    @Test
    void shouldRejectCountMatchedButNotClosingSnapshot() {
        LocalDate date = LocalDate.of(2026, 8, 28);
        MarketDailyTurnoverServiceImpl service = new MarketDailyTurnoverServiceImpl(
                marketDailyTurnoverMapper, stockDailyQuoteMapper, stockBasicService,
                tradeCalendarService, new MarketDailyTurnoverCalculator(), stockDailyQuoteService);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(stockBasicService.countSnapshot(date)).thenReturn(1);
        when(stockDailyQuoteMapper.countByTradeDate(date)).thenReturn(1);
        when(stockBasicService.findSnapshotBatch(date, "", 1000)).thenReturn(List.of(
                StockBasic.builder().stockCode("600000").build()));
        assertThrows(IllegalStateException.class, () -> service.synchronize(date));
        verify(stockDailyQuoteMapper, never()).selectMarketTurnoverRecords(List.of(date));
    }
}
