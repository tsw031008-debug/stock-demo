package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketDailyTurnoverCalculator;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
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

    @Test
    void shouldAggregateAndSaveCompleteTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        MarketDailyTurnoverServiceImpl service = new MarketDailyTurnoverServiceImpl(
                marketDailyTurnoverMapper,
                stockDailyQuoteMapper,
                stockBasicService,
                tradeCalendarService,
                new MarketDailyTurnoverCalculator()
        );
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(stockBasicService.countSnapshot(tradeDate)).thenReturn(2);
        when(stockDailyQuoteMapper.countByTradeDate(tradeDate)).thenReturn(2);
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
                new MarketDailyTurnoverCalculator()
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
}
