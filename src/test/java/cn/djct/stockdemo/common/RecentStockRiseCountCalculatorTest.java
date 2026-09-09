package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.vo.RecentStockRiseCountRespVo;
import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecentStockRiseCountCalculatorTest {

    private final RecentStockRiseCountCalculator calculator =
            new RecentStockRiseCountCalculator();

    @Test
    void shouldReturnTenDaysAndUseStrictFivePercentThreshold() {
        List<LocalDate> tradingDates = tradingDates();
        LocalDate tenDayBaseDate = tradingDates.get(9);
        LocalDate fiveDayBaseDate = tradingDates.get(14);
        LocalDate statisticsDate = tradingDates.get(19);
        List<StockClosePriceDto> closePrices = List.of(
                quote("000001", tenDayBaseDate, "100"),
                quote("000001", fiveDayBaseDate, "100"),
                quote("000001", statisticsDate, "105"),
                quote("600000", tenDayBaseDate, "100"),
                quote("600000", fiveDayBaseDate, "100"),
                quote("600000", statisticsDate, "105.01")
        );

        List<RecentStockRiseCountRespVo> result = calculator.calculate(
                tradingDates,
                closePrices
        );

        assertEquals(10, result.size());
        assertEquals(tradingDates.get(10), result.get(0).getTradeDate());
        assertEquals(statisticsDate, result.get(9).getTradeDate());
        assertEquals(1, result.get(9).getFiveDayRiseCount());
        assertEquals(1, result.get(9).getTenDayRiseCount());
    }

    @Test
    void shouldExcludeOnlyThePeriodWhoseBasePriceIsMissing() {
        List<LocalDate> tradingDates = tradingDates();
        LocalDate tenDayBaseDate = tradingDates.get(9);
        LocalDate fiveDayBaseDate = tradingDates.get(14);
        LocalDate statisticsDate = tradingDates.get(19);

        List<RecentStockRiseCountRespVo> result = calculator.calculate(
                tradingDates,
                List.of(
                        quote("000001", fiveDayBaseDate, "100"),
                        quote("000001", statisticsDate, "106"),
                        quote("600000", tenDayBaseDate, "100"),
                        quote("600000", statisticsDate, "106")
                )
        );

        assertEquals(1, result.get(9).getFiveDayRiseCount());
        assertEquals(1, result.get(9).getTenDayRiseCount());
    }

    @Test
    void shouldRejectDuplicateQuote() {
        List<LocalDate> tradingDates = tradingDates();
        LocalDate tradeDate = tradingDates.get(19);
        List<StockClosePriceDto> closePrices = List.of(
                quote("000001", tradeDate, "10"),
                quote("000001", tradeDate, "11")
        );

        assertThrows(
                IllegalStateException.class,
                () -> calculator.calculate(tradingDates, closePrices)
        );
    }

    @Test
    void shouldRejectNonPositiveClosePrice() {
        List<LocalDate> tradingDates = tradingDates();

        assertThrows(
                IllegalStateException.class,
                () -> calculator.calculate(
                        tradingDates,
                        List.of(quote("000001", tradingDates.get(19), "0"))
                )
        );
    }

    private List<LocalDate> tradingDates() {
        LocalDate startDate = LocalDate.of(2026, 8, 3);
        return IntStream.range(0, 20)
                .mapToObj(startDate::plusDays)
                .toList();
    }

    private StockClosePriceDto quote(String stockCode, LocalDate tradeDate, String closePrice) {
        return StockClosePriceDto.builder()
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .build();
    }
}
