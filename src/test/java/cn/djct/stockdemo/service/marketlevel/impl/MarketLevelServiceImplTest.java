package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.common.MarketLevelCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.MarketLevelDto;
import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.service.marketlevel.MarketLevelSourceService;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketLevelServiceImplTest {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");

    @Mock
    private StockDailyQuoteMapper stockDailyQuoteMapper;

    @Mock
    private MarketLevelSourceService marketLevelSourceService;

    @Mock
    private TradeCalendarService tradeCalendarService;

    private MarketLevelCalculator marketLevelCalculator;

    @BeforeEach
    void setUp() {
        marketLevelCalculator = new MarketLevelCalculator();
    }

    @Test
    void shouldUsePreviousTradingDayOnNonTradingDay() {
        LocalDate currentDate = LocalDate.of(2026, 8, 23);
        LocalDate statisticsDate = LocalDate.of(2026, 8, 21);
        Clock clock = Clock.fixed(Instant.parse("2026-08-23T04:00:00Z"), SHANGHAI_ZONE);
        MarketLevelServiceImpl service = createService(clock);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(false);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 1)).thenReturn(statisticsDate);
        prepareTurnovers(statisticsDate);

        MarketLevelDto result = service.getLatest();

        assertEquals(new BigDecimal("9000.00"), result.getCurrentTurnoverYi());
        assertEquals(new BigDecimal("8000.00"), result.getPreviousThreeDayAverageTurnoverYi());
        assertEquals(new BigDecimal("1.00"), result.getVolumeRatio());
        verify(stockDailyQuoteMapper).selectMarketTurnoverRecords(historicalTradeDates(statisticsDate));
    }

    @Test
    void shouldUseCurrentTradingDayAfterMarketOpen() {
        LocalDate currentDate = LocalDate.of(2026, 8, 24);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T02:00:00Z"), SHANGHAI_ZONE);
        MarketLevelServiceImpl service = createService(clock);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        prepareTurnovers(currentDate);

        service.getLatest();

        verify(stockDailyQuoteMapper).selectMarketTurnoverRecords(historicalTradeDates(currentDate));
    }

    @Test
    void shouldUsePreviousTradingDayBeforeMarketOpen() {
        LocalDate currentDate = LocalDate.of(2026, 8, 24);
        LocalDate statisticsDate = LocalDate.of(2026, 8, 21);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T01:00:00Z"), SHANGHAI_ZONE);
        MarketLevelServiceImpl service = createService(clock);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 1)).thenReturn(statisticsDate);
        prepareTurnovers(statisticsDate);

        service.getLatest();

        verify(stockDailyQuoteMapper).selectMarketTurnoverRecords(historicalTradeDates(statisticsDate));
    }

    @Test
    void shouldUsePreviousTradingDayAcrossHolidayAndYearBoundary() {
        LocalDate currentDate = LocalDate.of(2026, 1, 1);
        LocalDate statisticsDate = LocalDate.of(2025, 12, 31);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T04:00:00Z"), SHANGHAI_ZONE);
        MarketLevelServiceImpl service = createService(clock);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(false);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 1)).thenReturn(statisticsDate);
        prepareTurnovers(statisticsDate);

        service.getLatest();

        verify(stockDailyQuoteMapper).selectMarketTurnoverRecords(historicalTradeDates(statisticsDate));
    }

    @Test
    void shouldRejectMissingTurnoverAmountAfterServiceAggregation() {
        LocalDate currentDate = LocalDate.of(2026, 8, 24);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T02:00:00Z"), SHANGHAI_ZONE);
        MarketLevelServiceImpl service = createService(clock);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        List<LocalDate> tradeDates = prepareHistoricalTradeDates(currentDate);
        when(stockDailyQuoteMapper.selectMarketTurnoverRecords(tradeDates)).thenReturn(List.of(
                turnoverRecord(tradeDates.get(0), amountYuan("7000")),
                turnoverRecord(tradeDates.get(0), null),
                turnoverRecord(tradeDates.get(1), amountYuan("8000")),
                turnoverRecord(tradeDates.get(2), amountYuan("9000")),
                turnoverRecord(tradeDates.get(3), amountYuan("10000")),
                turnoverRecord(tradeDates.get(4), amountYuan("11000"))
        ));
        when(marketLevelSourceService.fetchCurrentTurnoverAmountYuan()).thenReturn(amountYuan("9000"));

        assertThrows(IllegalStateException.class, service::getLatest);
    }

    private MarketLevelServiceImpl createService(Clock clock) {
        return new MarketLevelServiceImpl(
                stockDailyQuoteMapper,
                marketLevelSourceService,
                tradeCalendarService,
                marketLevelCalculator,
                clock
        );
    }

    private void prepareTurnovers(LocalDate statisticsDate) {
        when(marketLevelSourceService.fetchCurrentTurnoverAmountYuan()).thenReturn(amountYuan("9000"));
        List<LocalDate> tradeDates = prepareHistoricalTradeDates(statisticsDate);
        List<MarketTurnoverRecordDto> records = new ArrayList<>();
        String[] dailyTurnoversYi = {"7000", "8000", "9000", "10000", "11000"};
        for (int index = 0; index < tradeDates.size(); index++) {
            BigDecimal halfAmountYuan = amountYuan(dailyTurnoversYi[index])
                    .divide(BigDecimal.valueOf(2));
            records.add(turnoverRecord(tradeDates.get(index), halfAmountYuan));
            records.add(turnoverRecord(tradeDates.get(index), halfAmountYuan));
        }
        when(stockDailyQuoteMapper.selectMarketTurnoverRecords(tradeDates)).thenReturn(records);
    }

    private List<LocalDate> prepareHistoricalTradeDates(LocalDate statisticsDate) {
        List<LocalDate> tradeDates = historicalTradeDates(statisticsDate);
        for (int offset = 1; offset <= tradeDates.size(); offset++) {
            when(tradeCalendarService.getPreviousTradingDay(statisticsDate, offset))
                    .thenReturn(tradeDates.get(offset - 1));
        }
        return tradeDates;
    }

    private List<LocalDate> historicalTradeDates(LocalDate statisticsDate) {
        return List.of(
                statisticsDate.minusDays(1),
                statisticsDate.minusDays(2),
                statisticsDate.minusDays(3),
                statisticsDate.minusDays(4),
                statisticsDate.minusDays(5)
        );
    }

    private MarketTurnoverRecordDto turnoverRecord(LocalDate tradeDate, BigDecimal amountYuan) {
        return MarketTurnoverRecordDto.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(amountYuan)
                .build();
    }

    private BigDecimal amountYuan(String amountYi) {
        return new BigDecimal(amountYi).multiply(ONE_HUNDRED_MILLION);
    }
}
