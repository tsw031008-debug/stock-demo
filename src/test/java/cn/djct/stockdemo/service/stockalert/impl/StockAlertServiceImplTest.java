package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.StockAlertCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockAlertServiceImplTest {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 24);

    @Mock
    private StockBasicService stockBasicService;
    @Mock
    private StockDailyQuoteSourceService stockDailyQuoteSourceService;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @Mock
    private StockAlertCalculator stockAlertCalculator;

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
    void shouldRejectNonTradingDay() {
        StockAlertServiceImpl service = createService("2026-08-24T02:00:00Z");
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findSpeedAlerts(1, 20)
        );

        assertEquals("当前为非交易日", exception.getMessage());
        verifyNoInteractions(stockBasicService, stockDailyQuoteSourceService);
    }

    @Test
    void shouldRejectTradingDayBeforeMarketOpen() {
        StockAlertServiceImpl service = createService("2026-08-24T01:00:00Z");
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findOpenBoardAlerts(1, 20)
        );

        assertEquals("当前不在交易时间", exception.getMessage());
        verifyNoInteractions(stockBasicService, stockDailyQuoteSourceService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-08-24T04:00:00Z",
            "2026-08-24T08:00:00Z"
    })
    void shouldAllowQueryAtLunchAndAfterMarketClose(String instant) {
        StockAlertServiceImpl service = createService(instant);
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findOpenBoardAlerts(1, 20)
        );

        assertEquals("当天股票清单不完整，minimum=3000，actual=0", exception.getMessage());
        verify(stockBasicService).countSnapshot(TRADE_DATE);
    }

    @Test
    void shouldShareQuoteSnapshotAndPaginateCalculatedResults() {
        StockAlertServiceImpl service = createService("2026-08-24T02:00:00Z");
        List<StockBasic> stocks = createStocks(3000);
        List<StockDailyQuote> quotes = createQuotes(3000);
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 1))
                .thenReturn(LocalDate.of(2026, 8, 21));
        when(tradeCalendarService.getPreviousTradingDay(TRADE_DATE, 2))
                .thenReturn(LocalDate.of(2026, 8, 20));
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(3000);
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "", 1000))
                .thenReturn(stocks.subList(0, 1000));
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "000999", 1000))
                .thenReturn(stocks.subList(1000, 2000));
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "001999", 1000))
                .thenReturn(stocks.subList(2000, 3000));
        when(stockDailyQuoteSourceService.fetchAll(TRADE_DATE, stocks)).thenReturn(quotes);
        when(stockDailyQuoteMapper.selectClosePricesByTradeDates(anyList())).thenReturn(List.of());
        when(stockAlertCalculator.calculateSpeedAlerts(anyList(), anyList(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        speedAlert("000001"),
                        speedAlert("000002"),
                        speedAlert("000003")
                ));
        when(stockAlertCalculator.calculateOpenBoardAlerts(quotes)).thenReturn(List.of(
                StockOpenBoardAlertDto.builder().stockCode("000001").build()
        ));

        PageDto<StockSpeedAlertDto> speedPage = service.findSpeedAlerts(2, 2);
        PageDto<StockOpenBoardAlertDto> openBoardPage = service.findOpenBoardAlerts(1, 20);

        assertEquals(3, speedPage.getTotal());
        assertEquals(List.of("000003"), speedPage.getRecords().stream()
                .map(StockSpeedAlertDto::getStockCode)
                .toList());
        assertEquals(1, openBoardPage.getTotal());
        verify(stockDailyQuoteSourceService).fetchAll(TRADE_DATE, stocks);
    }

    @Test
    void shouldRejectInvalidPageSizeBeforeReadingData() {
        StockAlertServiceImpl service = createService("2026-08-24T02:00:00Z");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findSpeedAlerts(1, 101)
        );

        assertEquals("每页数量必须在1到100之间", exception.getMessage());
        verifyNoInteractions(tradeCalendarService, stockBasicService, stockDailyQuoteSourceService);
    }

    private StockAlertServiceImpl createService(String instant) {
        return new StockAlertServiceImpl(
                stockBasicService,
                stockDailyQuoteSourceService,
                tradeCalendarService,
                stockDailyQuoteMapper,
                stockAlertCalculator,
                Clock.fixed(Instant.parse(instant), SHANGHAI_ZONE)
        );
    }

    private List<StockBasic> createStocks(int count) {
        List<StockBasic> stocks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            stocks.add(StockBasic.builder()
                    .stockCode(String.format("%06d", index))
                    .stockName("股票" + index)
                    .build());
        }
        return stocks;
    }

    private List<StockDailyQuote> createQuotes(int count) {
        List<StockDailyQuote> quotes = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            quotes.add(StockDailyQuote.builder()
                    .stockCode(String.format("%06d", index))
                    .closePrice(BigDecimal.TEN)
                    .build());
        }
        return quotes;
    }

    private StockSpeedAlertDto speedAlert(String stockCode) {
        return StockSpeedAlertDto.builder()
                .stockCode(stockCode)
                .build();
    }
}
