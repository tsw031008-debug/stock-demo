package cn.djct.stockdemo.service.indexdivergence.impl;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexDivergenceOverviewDto;
import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexDivergenceQueryServiceImplTest {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;

    @Mock
    private IndexDivergenceSignalMapper indexDivergenceSignalMapper;

    @Test
    void shouldReturnCurrentTradingDayCurveAndSignals() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        LocalDateTime quoteTime = LocalDateTime.of(tradeDate, LocalTime.of(9, 31));
        IndexMinuteQuote quote = quote(quoteTime);
        IndexDivergenceSignal signal = signal(quoteTime.plusMinutes(20));
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(tradeDate, LocalTime.of(9, 31)),
                LocalDateTime.of(tradeDate, LocalTime.of(15, 0))
        )).thenReturn(List.of(quote));
        when(indexDivergenceSignalMapper.selectByIndexCodeAndSignalTimeRange(
                "000001",
                tradeDate.atStartOfDay(),
                tradeDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(signal));
        IndexDivergenceQueryServiceImpl service = serviceAt("2026-08-28T02:00:00Z");

        IndexDivergenceOverviewDto result = service.getLatest();

        assertEquals(tradeDate, result.getTradeDate());
        assertEquals(new BigDecimal("3820.10"), result.getPreviousClosePrice());
        assertEquals(1, result.getCurveData().size());
        assertEquals(quoteTime, result.getCurveData().get(0).getQuoteTime());
        assertEquals(1, result.getSignalData().size());
        assertEquals(IndexDivergenceSignalType.MACD_BOTTOM,
                result.getSignalData().get(0).getSignalType());
    }

    @Test
    void shouldReturnPreviousTradingDayBeforeMarketOpen() {
        LocalDate currentDate = LocalDate.of(2026, 8, 28);
        LocalDate previousTradeDate = LocalDate.of(2026, 8, 27);
        when(tradeCalendarService.isTradingDay(currentDate)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(currentDate, 1))
                .thenReturn(previousTradeDate);
        when(indexMinuteQuoteMapper.selectByIndexCodeAndQuoteTimeRange(
                "000001",
                LocalDateTime.of(previousTradeDate, LocalTime.of(9, 31)),
                LocalDateTime.of(previousTradeDate, LocalTime.of(15, 0))
        )).thenReturn(List.of());
        when(indexDivergenceSignalMapper.selectByIndexCodeAndSignalTimeRange(
                "000001",
                previousTradeDate.atStartOfDay(),
                previousTradeDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        IndexDivergenceQueryServiceImpl service = serviceAt("2026-08-28T01:00:00Z");

        IndexDivergenceOverviewDto result = service.getLatest();

        assertEquals(previousTradeDate, result.getTradeDate());
        assertEquals(List.of(), result.getCurveData());
        assertEquals(List.of(), result.getSignalData());
        verify(tradeCalendarService).getPreviousTradingDay(currentDate, 1);
    }

    private IndexDivergenceQueryServiceImpl serviceAt(String instant) {
        return new IndexDivergenceQueryServiceImpl(
                tradeCalendarService,
                indexMinuteQuoteMapper,
                indexDivergenceSignalMapper,
                Clock.fixed(Instant.parse(instant), SHANGHAI_ZONE)
        );
    }

    private IndexMinuteQuote quote(LocalDateTime quoteTime) {
        return IndexMinuteQuote.builder()
                .indexCode("000001")
                .indexName("上证指数")
                .tradeDate(quoteTime.toLocalDate())
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal("3850.12"))
                .previousClosePrice(new BigDecimal("3820.10"))
                .build();
    }

    private IndexDivergenceSignal signal(LocalDateTime signalTime) {
        return IndexDivergenceSignal.builder()
                .indexCode("000001")
                .signalType(IndexDivergenceSignalType.MACD_BOTTOM)
                .signalTime(signalTime)
                .previousIntervalStartTime(signalTime.minusMinutes(20))
                .previousIntervalEndTime(signalTime.minusMinutes(15))
                .currentIntervalStartTime(signalTime.minusMinutes(5))
                .currentIntervalEndTime(signalTime)
                .previousPriceExtreme(new BigDecimal("3860.00"))
                .currentPriceExtreme(new BigDecimal("3850.00"))
                .previousMacdExtreme(new BigDecimal("-2.00"))
                .currentMacdExtreme(new BigDecimal("-1.00"))
                .previousDifExtreme(new BigDecimal("-1.50"))
                .currentDifExtreme(new BigDecimal("-0.80"))
                .build();
    }
}
