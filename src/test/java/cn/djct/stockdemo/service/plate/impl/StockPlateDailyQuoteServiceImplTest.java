package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.common.StockPlateCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.entity.StockPlateDailyQuote;
import cn.djct.stockdemo.service.plate.StockPlateService;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockPlateDailyQuoteServiceImplTest {

    private static final LocalDate END_DATE = LocalDate.of(2026, 1, 6);
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 1, 5);

    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private StockBasicService stockBasicService;
    @Mock
    private StockDailyQuoteService stockDailyQuoteService;
    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private StockPlateService stockPlateService;
    @Mock
    private StockPlateDailyQuoteMapper stockPlateDailyQuoteMapper;
    @Mock
    private StockPlateCalculator stockPlateCalculator;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldRejectIncompleteStockDailyQuote() {
        when(tradeCalendarService.isTradingDay(END_DATE)).thenReturn(true);
        when(stockBasicService.countSnapshot(END_DATE)).thenReturn(3000);
        when(stockDailyQuoteService.countByTradeDate(END_DATE)).thenReturn(2999);
        StockPlateDailyQuoteServiceImpl service = createService();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.synchronize(END_DATE)
        );

        assertEquals("当天股票日行情不完整，expected=3000，actual=2999", exception.getMessage());
        verifyNoInteractions(stockPlateService, stockPlateCalculator);
    }

    @Test
    void shouldBackfillTradingDaysInSequenceAsPartial() {
        List<LocalDate> tradingDays = List.of(FIRST_DATE, END_DATE);
        List<StockPlateMemberDto> members = List.of(StockPlateMemberDto.builder()
                .plateId(1L)
                .plateName("科技-概")
                .stockCode("000001")
                .build());
        List<StockDailyQuote> firstStockQuotes = List.of(stockQuote(FIRST_DATE));
        List<StockDailyQuote> secondStockQuotes = List.of(stockQuote(END_DATE));
        List<StockPlateDailyQuote> firstPlateQuotes = List.of(plateQuote(FIRST_DATE, "1010.0000"));
        List<StockPlateDailyQuote> secondPlateQuotes = List.of(plateQuote(END_DATE, "1020.0000"));
        when(tradeCalendarService.isTradingDay(END_DATE)).thenReturn(true);
        when(tradeCalendarService.getTradingDays(LocalDate.of(2026, 1, 1), END_DATE))
                .thenReturn(tradingDays);
        when(stockPlateService.findActiveMembers()).thenReturn(members);
        when(stockDailyQuoteMapper.selectForPlateCalculation(FIRST_DATE)).thenReturn(firstStockQuotes);
        when(stockDailyQuoteMapper.selectForPlateCalculation(END_DATE)).thenReturn(secondStockQuotes);
        when(stockPlateCalculator.calculateDailyQuotes(
                FIRST_DATE, members, firstStockQuotes, Map.of(), true, "PARTIAL"
        )).thenReturn(firstPlateQuotes);
        when(stockPlateCalculator.calculateDailyQuotes(
                eq(END_DATE), eq(members), eq(secondStockQuotes),
                eq(Map.of(1L, new BigDecimal("1010.0000"))), eq(false), eq("PARTIAL")
        )).thenReturn(secondPlateQuotes);
        StockPlateDailyQuoteServiceImpl service = createService();

        assertEquals(2, service.backfillCurrentYear(END_DATE));
    }

    /**
     * 创建板块日线服务。
     */
    private StockPlateDailyQuoteServiceImpl createService() {
        return new StockPlateDailyQuoteServiceImpl(
                tradeCalendarService,
                stockBasicService,
                stockDailyQuoteService,
                stockDailyQuoteMapper,
                stockPlateService,
                stockPlateDailyQuoteMapper,
                stockPlateCalculator
        );
    }

    /**
     * 创建股票日行情原始数据。
     */
    private StockDailyQuote stockQuote(LocalDate tradeDate) {
        return StockDailyQuote.builder()
                .stockCode("000001")
                .tradeDate(tradeDate)
                .changePercent(BigDecimal.ONE)
                .turnoverAmountYuan(BigDecimal.TEN)
                .build();
    }

    /**
     * 创建板块日线计算结果。
     */
    private StockPlateDailyQuote plateQuote(LocalDate tradeDate, String closePrice) {
        return StockPlateDailyQuote.builder()
                .plateId(1L)
                .plateName("科技-概")
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .build();
    }
}
