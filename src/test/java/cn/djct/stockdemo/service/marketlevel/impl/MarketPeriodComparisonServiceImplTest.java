package cn.djct.stockdemo.service.marketlevel.impl;

import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.pojo.vo.MarketPeriodComparisonRespVo;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketPeriodComparisonServiceImplTest {

    @Mock
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Test
    void shouldUseFiveWeeksAgoAndPreviousActualTradingWeek() {
        LocalDate statisticsDate = LocalDate.of(2026, 8, 20);
        MarketPeriodComparisonServiceImpl service = new MarketPeriodComparisonServiceImpl(
                marketDailyTurnoverMapper,
                tradeCalendarService
        );
        when(marketDailyTurnoverMapper.selectLatestCompleteTradeDate()).thenReturn(statisticsDate);

        preparePeriod(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 19),
                dates(2026, 7, 13, 14, 15, 16, 17), "100");
        when(tradeCalendarService.getTradingDays(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16))).thenReturn(List.of());
        preparePeriod(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9),
                dates(2026, 8, 3, 4, 5, 6, 7), "200");
        preparePeriod(LocalDate.of(2026, 8, 17), statisticsDate,
                dates(2026, 8, 17, 18, 19, 20), "300");
        preparePeriod(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31),
                List.of(LocalDate.of(2025, 8, 1)), "400");
        preparePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                List.of(LocalDate.of(2026, 7, 1)), "500");
        preparePeriod(LocalDate.of(2026, 8, 1), statisticsDate,
                List.of(statisticsDate), "600");

        MarketPeriodComparisonRespVo result = service.getLatest();

        assertEquals(statisticsDate, result.getStatisticsTradeDate());
        assertEquals(List.of("YOY", "MOM", "CURRENT"), result.getWeekly().stream()
                .map(item -> item.getComparisonType()).toList());
        assertEquals(new BigDecimal("100.00"), result.getWeekly().get(0).getAverageTurnoverYi());
        assertEquals(LocalDate.of(2026, 7, 13), result.getWeekly().get(0).getStartDate());
        assertEquals(new BigDecimal("200.00"), result.getWeekly().get(1).getAverageTurnoverYi());
        assertEquals(LocalDate.of(2026, 8, 3), result.getWeekly().get(1).getStartDate());
        assertEquals(4, result.getWeekly().get(2).getTradingDayCount());
        assertEquals(List.of("YOY", "MOM", "CURRENT"), result.getMonthly().stream()
                .map(item -> item.getComparisonType()).toList());
        assertEquals(new BigDecimal("400.00"), result.getMonthly().get(0).getAverageTurnoverYi());
        assertEquals(new BigDecimal("500.00"), result.getMonthly().get(1).getAverageTurnoverYi());
        assertEquals(new BigDecimal("600.00"), result.getMonthly().get(2).getAverageTurnoverYi());
    }

    private void preparePeriod(LocalDate startDate, LocalDate endDate,
                               List<LocalDate> tradingDays, String amountYi) {
        when(tradeCalendarService.getTradingDays(startDate, endDate)).thenReturn(tradingDays);
        when(marketDailyTurnoverMapper.selectCompleteByDateRange(startDate, endDate))
                .thenReturn(tradingDays.stream()
                        .map(date -> daily(date, amountYi))
                        .toList());
    }

    private MarketDailyTurnover daily(LocalDate tradeDate, String amountYi) {
        return MarketDailyTurnover.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(new BigDecimal(amountYi).multiply(new BigDecimal("100000000")))
                .stockCount(5000)
                .amountRecordCount(5000)
                .dataSource("STOCK_DAILY_QUOTE")
                .dataStatus("COMPLETE")
                .build();
    }

    private List<LocalDate> dates(int year, int month, int... days) {
        return java.util.Arrays.stream(days)
                .mapToObj(day -> LocalDate.of(year, month, day))
                .toList();
    }
}
