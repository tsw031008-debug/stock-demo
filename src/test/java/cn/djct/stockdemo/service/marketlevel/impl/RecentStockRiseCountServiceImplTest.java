package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.RecentStockRiseCountCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.vo.RecentStockRiseCountRespVo;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentStockRiseCountServiceImplTest {

    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private RecentStockRiseCountCalculator calculator;

    @Test
    void shouldLoadTwentyTradingDaysAndCalculateRecentTenDays() {
        List<LocalDate> tradingDates = tradingDates(20);
        LocalDate startDate = tradingDates.get(0);
        LocalDate statisticsDate = tradingDates.get(19);
        List<StockClosePriceDto> closePrices = completeTargetQuotes(tradingDates);
        List<RecentStockRiseCountRespVo> expected = List.of(
                RecentStockRiseCountRespVo.builder()
                        .tradeDate(statisticsDate)
                        .fiveDayRiseCount(100)
                        .tenDayRiseCount(200)
                        .build()
        );
        RecentStockRiseCountServiceImpl service = service();
        when(stockDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 19))
                .thenReturn(startDate);
        when(tradeCalendarService.getTradingDays(startDate, statisticsDate))
                .thenReturn(tradingDates);
        when(stockDailyQuoteMapper.selectClosePricesByTradeDates(tradingDates))
                .thenReturn(closePrices);
        when(calculator.calculate(tradingDates, closePrices)).thenReturn(expected);

        List<RecentStockRiseCountRespVo> result = service.getLatest();

        assertEquals(expected, result);
        verify(stockDailyQuoteMapper).selectClosePricesByTradeDates(tradingDates);
        verify(calculator).calculate(tradingDates, closePrices);
    }

    @Test
    void shouldRejectWhenNoDailyQuoteExists() {
        RecentStockRiseCountServiceImpl service = service();
        when(stockDailyQuoteMapper.selectLatestTradeDate()).thenReturn(null);

        assertThrows(IllegalStateException.class, service::getLatest);

        verifyNoInteractions(tradeCalendarService, calculator);
    }

    @Test
    void shouldRejectWhenTradingCalendarIsIncomplete() {
        List<LocalDate> tradingDates = tradingDates(19);
        LocalDate statisticsDate = LocalDate.of(2026, 8, 28);
        LocalDate startDate = tradingDates.get(0);
        RecentStockRiseCountServiceImpl service = service();
        when(stockDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 19))
                .thenReturn(startDate);
        when(tradeCalendarService.getTradingDays(startDate, statisticsDate))
                .thenReturn(tradingDates);

        assertThrows(IllegalStateException.class, service::getLatest);

        verifyNoInteractions(calculator);
    }

    @Test
    void shouldRejectIncompleteTargetTradeDate() {
        List<LocalDate> tradingDates = tradingDates(20);
        LocalDate startDate = tradingDates.get(0);
        LocalDate statisticsDate = tradingDates.get(19);
        List<StockClosePriceDto> incompleteQuotes = List.of(
                quote("000001", tradingDates.get(10))
        );
        RecentStockRiseCountServiceImpl service = service();
        when(stockDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 19))
                .thenReturn(startDate);
        when(tradeCalendarService.getTradingDays(startDate, statisticsDate))
                .thenReturn(tradingDates);
        when(stockDailyQuoteMapper.selectClosePricesByTradeDates(tradingDates))
                .thenReturn(incompleteQuotes);

        assertThrows(IllegalStateException.class, service::getLatest);

        verifyNoInteractions(calculator);
    }

    private RecentStockRiseCountServiceImpl service() {
        return new RecentStockRiseCountServiceImpl(
                stockDailyQuoteMapper,
                tradeCalendarService,
                calculator
        );
    }

    private List<LocalDate> tradingDates(int count) {
        LocalDate startDate = LocalDate.of(2026, 8, 3);
        return IntStream.range(0, count)
                .mapToObj(startDate::plusDays)
                .toList();
    }

    private List<StockClosePriceDto> completeTargetQuotes(List<LocalDate> tradingDates) {
        List<StockClosePriceDto> quotes = new ArrayList<>();
        for (LocalDate tradeDate : tradingDates.subList(10, 20)) {
            for (int stockIndex = 0; stockIndex < 3000; stockIndex++) {
                quotes.add(quote(String.format("%06d", stockIndex), tradeDate));
            }
        }
        return quotes;
    }

    private StockClosePriceDto quote(String stockCode, LocalDate tradeDate) {
        return StockClosePriceDto.builder()
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .closePrice(BigDecimal.TEN)
                .build();
    }
}
