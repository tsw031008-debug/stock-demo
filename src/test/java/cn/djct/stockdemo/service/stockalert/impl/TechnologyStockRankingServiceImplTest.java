package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.TechnologyStockRankingCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.TechnologyStockQuoteDto;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankingRespVo;
import cn.djct.stockdemo.service.stockalert.TechnologyStockPoolSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnologyStockRankingServiceImplTest {

    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 9, 7);
    private static final LocalDate FIVE_DAY_BASE_DATE = LocalDate.of(2026, 8, 31);
    private static final LocalDate TEN_DAY_BASE_DATE = LocalDate.of(2026, 8, 24);
    private static final LocalDate FIFTEEN_DAY_BASE_DATE = LocalDate.of(2026, 8, 17);
    private static final LocalDate TWENTY_DAY_BASE_DATE = LocalDate.of(2026, 8, 10);
    private static final LocalDate SIXTY_DAY_BASE_DATE = LocalDate.of(2026, 6, 12);

    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private TechnologyStockPoolSourceService stockPoolSourceService;
    @Mock
    private TechnologyStockRankingCalculator calculator;

    @Test
    void shouldLoadTechnologyQuotesForSixTradingDatesOnce() {
        TechnologyStockRankingServiceImpl service = service();
        List<String> stockCodes = List.of("000001", "600000");
        List<LocalDate> targetDates = targetDates();
        List<TechnologyStockQuoteDto> quotes = List.of(
                TechnologyStockQuoteDto.builder()
                        .stockCode("000001")
                        .tradeDate(STATISTICS_DATE)
                        .build()
        );
        TechnologyStockRankingRespVo expected = TechnologyStockRankingRespVo.builder()
                .statisticsDate(STATISTICS_DATE)
                .build();
        when(stockDailyQuoteMapper.selectLatestTradeDate())
                .thenReturn(STATISTICS_DATE);
        mockTradingDayOffsets();
        when(stockPoolSourceService.fetchStockCodes()).thenReturn(stockCodes);
        when(stockDailyQuoteMapper.selectTechnologyStockQuotes(stockCodes, targetDates))
                .thenReturn(quotes);
        when(calculator.calculate(
                STATISTICS_DATE,
                FIVE_DAY_BASE_DATE,
                TEN_DAY_BASE_DATE,
                FIFTEEN_DAY_BASE_DATE,
                TWENTY_DAY_BASE_DATE,
                SIXTY_DAY_BASE_DATE,
                quotes
        )).thenReturn(expected);

        TechnologyStockRankingRespVo result = service.getLatest();

        assertEquals(expected, result);
        verify(stockDailyQuoteMapper).selectTechnologyStockQuotes(stockCodes, targetDates);
        verify(stockDailyQuoteMapper).selectLatestTradeDate();
        org.mockito.Mockito.verifyNoMoreInteractions(stockDailyQuoteMapper);
        verify(calculator).calculate(
                STATISTICS_DATE,
                FIVE_DAY_BASE_DATE,
                TEN_DAY_BASE_DATE,
                FIFTEEN_DAY_BASE_DATE,
                TWENTY_DAY_BASE_DATE,
                SIXTY_DAY_BASE_DATE,
                quotes
        );
    }

    @Test
    void shouldRejectWhenNoDailyQuoteExists() {
        TechnologyStockRankingServiceImpl service = service();
        when(stockDailyQuoteMapper.selectLatestTradeDate())
                .thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::getLatest
        );

        assertEquals("没有可用的股票日行情数据", exception.getMessage());
        verifyNoInteractions(tradeCalendarService, stockPoolSourceService, calculator);
    }

    private TechnologyStockRankingServiceImpl service() {
        return new TechnologyStockRankingServiceImpl(
                stockDailyQuoteMapper,
                tradeCalendarService,
                stockPoolSourceService,
                calculator
        );
    }

    private List<LocalDate> targetDates() {
        return List.of(
                STATISTICS_DATE,
                FIVE_DAY_BASE_DATE,
                TEN_DAY_BASE_DATE,
                FIFTEEN_DAY_BASE_DATE,
                TWENTY_DAY_BASE_DATE,
                SIXTY_DAY_BASE_DATE
        );
    }

    private void mockTradingDayOffsets() {
        when(tradeCalendarService.getPreviousTradingDay(STATISTICS_DATE, 5))
                .thenReturn(FIVE_DAY_BASE_DATE);
        when(tradeCalendarService.getPreviousTradingDay(STATISTICS_DATE, 10))
                .thenReturn(TEN_DAY_BASE_DATE);
        when(tradeCalendarService.getPreviousTradingDay(STATISTICS_DATE, 15))
                .thenReturn(FIFTEEN_DAY_BASE_DATE);
        when(tradeCalendarService.getPreviousTradingDay(STATISTICS_DATE, 20))
                .thenReturn(TWENTY_DAY_BASE_DATE);
        when(tradeCalendarService.getPreviousTradingDay(STATISTICS_DATE, 60))
                .thenReturn(SIXTY_DAY_BASE_DATE);
    }
}
