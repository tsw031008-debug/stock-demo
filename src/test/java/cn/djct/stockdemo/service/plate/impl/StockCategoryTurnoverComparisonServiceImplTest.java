package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverComparisonDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.dto.StockTurnoverByDateDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.plate.StockCustomPlateService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCategoryTurnoverComparisonServiceImplTest {

    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 8, 28);
    private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 8, 27);
    private static final List<String> STOCK_CODES = List.of(
            "600001", "600002", "600003", "600004"
    );
    private static final Clock OPENED_MARKET_CLOCK = Clock.fixed(
            Instant.parse("2026-08-28T02:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private StockCustomPlateService stockCustomPlateService;
    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private StockDailyQuoteSourceService stockDailyQuoteSourceService;
    @Mock
    private TradeCalendarService tradeCalendarService;

    private StockCategoryTurnoverComparisonServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StockCategoryTurnoverComparisonServiceImpl(
                stockCustomPlateService,
                stockDailyQuoteMapper,
                stockDailyQuoteSourceService,
                tradeCalendarService,
                OPENED_MARKET_CLOCK
        );
    }

    private void stubTradingDayAndMembers() {
        when(tradeCalendarService.isTradingDay(CURRENT_DATE)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(CURRENT_DATE, 1)).thenReturn(PREVIOUS_DATE);
        when(stockCustomPlateService.findActiveMemberRelations()).thenReturn(List.of(
                relation("CYCLE", 1L, "600001"),
                relation("FINANCE", 2L, "600002"),
                relation("TECHNOLOGY", 3L, "600003"),
                relation("TECHNOLOGY", 4L, "600003"),
                relation("CONSUMPTION", 5L, "600004")
        ));
    }

    @Test
    void shouldFetchOnlyCategoryStocksAndCompareWithStoredPreviousDay() {
        stubTradingDayAndMembers();
        when(stockDailyQuoteSourceService.fetchByCodes(CURRENT_DATE, STOCK_CODES))
                .thenReturn(List.of(
                        currentQuote("600001", "100000000"),
                        currentQuote("600002", "200000000"),
                        currentQuote("600003", "300000000"),
                        currentQuote("600004", "400000000")
                ));
        when(stockDailyQuoteMapper.selectTurnoversByTradeDates(
                List.of(PREVIOUS_DATE),
                STOCK_CODES
        )).thenReturn(previousTurnovers());

        StockCategoryTurnoverComparisonDto result = service.getCurrent();

        assertEquals(CURRENT_DATE, result.getStatisticsDate());
        assertEquals(PREVIOUS_DATE, result.getPreviousTradeDate());
        assertEquals(List.of("CYCLE", "FINANCE", "TECHNOLOGY", "CONSUMPTION"),
                result.getCategories().stream().map(item -> item.getCategoryCode()).toList());
        assertEquals(1, result.getCategories().get(2).getStockCount());
        assertEquals(new BigDecimal("3.00"), result.getCategories().get(2).getCurrentTurnoverYi());
        assertEquals(new BigDecimal("1.50"), result.getCategories().get(2).getPreviousTurnoverYi());
        verify(stockDailyQuoteSourceService).fetchByCodes(CURRENT_DATE, STOCK_CODES);
    }

    @Test
    void shouldRejectRealtimeQuoteFromPreviousDate() {
        stubTradingDayAndMembers();
        List<StockDailyQuote> quotes = new ArrayList<>(List.of(
                currentQuote("600001", "100000000"),
                currentQuote("600002", "200000000"),
                currentQuote("600003", "300000000"),
                currentQuote("600004", "400000000")
        ));
        quotes.get(2).setQuoteTime(PREVIOUS_DATE.atTime(15, 0));
        when(stockDailyQuoteSourceService.fetchByCodes(CURRENT_DATE, STOCK_CODES)).thenReturn(quotes);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::getCurrent
        );

        assertTrue(exception.getMessage().contains("600003"));
        assertTrue(exception.getMessage().contains("不是当前交易日"));
    }

    @Test
    void shouldRejectMissingPreviousTurnoverInsteadOfReturningZero() {
        stubTradingDayAndMembers();
        when(stockDailyQuoteSourceService.fetchByCodes(CURRENT_DATE, STOCK_CODES))
                .thenReturn(List.of(
                        currentQuote("600001", "100000000"),
                        currentQuote("600002", "200000000"),
                        currentQuote("600003", "300000000"),
                        currentQuote("600004", "400000000")
                ));
        List<StockTurnoverByDateDto> incomplete = new ArrayList<>(previousTurnovers());
        incomplete.remove(2);
        when(stockDailyQuoteMapper.selectTurnoversByTradeDates(
                List.of(PREVIOUS_DATE),
                STOCK_CODES
        )).thenReturn(incomplete);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::getCurrent
        );

        assertTrue(exception.getMessage().contains("600003"));
        assertTrue(exception.getMessage().contains(PREVIOUS_DATE.toString()));
    }

    @Test
    void shouldRejectNonTradingDayBeforeRequestingRealtimeQuotes() {
        when(tradeCalendarService.isTradingDay(CURRENT_DATE)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::getCurrent
        );

        assertEquals("当前为非交易日", exception.getMessage());
    }

    @Test
    void shouldRejectTradingDayBeforeMarketOpen() {
        Clock beforeOpenClock = Clock.fixed(
                Instant.parse("2026-08-28T01:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        service = new StockCategoryTurnoverComparisonServiceImpl(
                stockCustomPlateService,
                stockDailyQuoteMapper,
                stockDailyQuoteSourceService,
                tradeCalendarService,
                beforeOpenClock
        );
        when(tradeCalendarService.isTradingDay(CURRENT_DATE)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::getCurrent
        );

        assertEquals("当前交易日尚未开盘", exception.getMessage());
    }

    private StockCustomPlateMemberRelationDto relation(
            String categoryCode,
            Long plateId,
            String stockCode
    ) {
        return StockCustomPlateMemberRelationDto.builder()
                .categoryCode(categoryCode)
                .customPlateId(plateId)
                .plateName("测试板块")
                .stockCode(stockCode)
                .build();
    }

    private StockDailyQuote currentQuote(String stockCode, String amount) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .quoteTime(LocalDateTime.of(2026, 8, 28, 10, 0))
                .turnoverAmountYuan(new BigDecimal(amount))
                .build();
    }

    private List<StockTurnoverByDateDto> previousTurnovers() {
        return List.of(
                turnover("600001", "50000000"),
                turnover("600002", "100000000"),
                turnover("600003", "150000000"),
                turnover("600004", "200000000")
        );
    }

    private StockTurnoverByDateDto turnover(String stockCode, String amount) {
        return StockTurnoverByDateDto.builder()
                .stockCode(stockCode)
                .tradeDate(PREVIOUS_DATE)
                .turnoverAmountYuan(new BigDecimal(amount))
                .build();
    }
}
