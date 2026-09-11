package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlatformBreakoutCalculatorTest {
    private final PlatformBreakoutCalculator calculator = new PlatformBreakoutCalculator();

    @Test
    void shouldMatchAllConditions() {
        assertTrue(calculator.matches(quotes()));
    }

    @ParameterizedTest
    @ValueSource(ints = {174, 194})
    void shouldIncludeBothPlatformEndpoints(int index) {
        var quotes = quotes();
        quotes.get(index).setHighPrice(new BigDecimal("111"));
        assertFalse(calculator.matches(quotes));
    }

    @ParameterizedTest
    @ValueSource(ints = {173, 195})
    void shouldExcludeAdjacentDaysFromPlatform(int index) {
        var quotes = quotes();
        quotes.get(index).setHighPrice(new BigDecimal("111"));
        assertTrue(calculator.matches(quotes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "101"})
    void shouldRejectYesterdayAtOrAboveTodaysPlatform(String close) {
        var quotes = quotes();
        // T-26曾有更高点，不能改用昨日滚动平台让昨收通过。
        quotes.get(173).setHighPrice(new BigDecimal("105"));
        quotes.get(198).setClosePrice(new BigDecimal(close));
        quotes.get(198).setHighPrice(new BigDecimal(close));
        assertFalse(calculator.matches(quotes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"99", "100"})
    void shouldRejectTodayAtOrBelowPlatform(String close) {
        var quotes = quotes();
        quotes.get(199).setClosePrice(new BigDecimal(close));
        assertFalse(calculator.matches(quotes));
    }

    @Test
    void shouldRequireStrictMovingAverageAndAdjustmentComparisons() {
        var quotes = quotes();
        quotes.forEach(q -> q.setClosePrice(new BigDecimal("80")));
        assertFalse(calculator.matches(quotes));
        quotes = quotes();
        for (int i = 140; i < 170; i++) {
            quotes.get(i).setClosePrice(new BigDecimal("95"));
            quotes.get(i).setHighPrice(new BigDecimal("95"));
        }
        assertFalse(calculator.matches(quotes));
        quotes = quotes();
        quotes.get(195).setClosePrice(new BigDecimal("90"));
        // 最近10日都不低于各自MA10，均线相等不能算调整。
        assertFalse(calculator.matches(quotes));
    }

    @Test
    void shouldUseTenDayAdjustmentWindowIncludingToday() {
        var quotes = quotes();
        quotes.get(195).setClosePrice(new BigDecimal("90"));
        quotes.get(190).setClosePrice(new BigDecimal("85"));
        assertTrue(calculator.matches(quotes));
        quotes.get(190).setClosePrice(new BigDecimal("90"));
        quotes.get(189).setClosePrice(new BigDecimal("85"));
        assertFalse(calculator.matches(quotes));
    }

    @Test
    void shouldIncludeFirstAndCurrentDayInHighest200AndIncludeNinetyPercent() {
        var quotes = quotes();
        quotes.get(0).setHighPrice(new BigDecimal("120"));
        quotes.get(199).setClosePrice(new BigDecimal("108"));
        assertTrue(calculator.matches(quotes));
        quotes.get(199).setClosePrice(new BigDecimal("107.9999"));
        assertFalse(calculator.matches(quotes));
        quotes = quotes();
        quotes.get(199).setHighPrice(new BigDecimal("130"));
        assertFalse(calculator.matches(quotes));
    }

    @Test
    void shouldRejectMissingAndInvalidPricesInsteadOfSilentlyNotSelecting() {
        var quotes = quotes();
        assertThrows(IllegalArgumentException.class, () -> calculator.matches(quotes.subList(1, 200)));
        quotes.get(0).setHighPrice(null);
        assertThrows(IllegalStateException.class, () -> calculator.matches(quotes));
        quotes.get(0).setHighPrice(BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> calculator.matches(quotes));
        quotes.get(0).setClosePrice(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> calculator.matches(quotes));
        quotes.get(0).setClosePrice(null);
        assertThrows(IllegalStateException.class, () -> calculator.matches(quotes));
        quotes.set(0, null);
        assertThrows(IllegalStateException.class, () -> calculator.matches(quotes));
    }

    @Test
    void shouldReuseGainFormulaForPositiveZeroAndNegativeReturns() {
        var calculator = new StockAlertCalculator();
        assertEquals(0, new BigDecimal("10").compareTo(calculator.calculateGain(new BigDecimal("110"), new BigDecimal("100"))));
        assertEquals(0, BigDecimal.ZERO.compareTo(calculator.calculateGain(BigDecimal.TEN, BigDecimal.TEN)));
        assertEquals(0, new BigDecimal("-10").compareTo(calculator.calculateGain(new BigDecimal("90"), new BigDecimal("100"))));
    }

    static List<StockDailyQuote> quotes() {
        List<StockDailyQuote> result = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            BigDecimal close = new BigDecimal(i >= 170 ? "90" : "80");
            result.add(StockDailyQuote.builder().tradeDate(LocalDate.of(2025, 1, 1).plusDays(i))
                    .closePrice(close).highPrice(close.add(BigDecimal.ONE)).build());
        }
        result.get(174).setHighPrice(new BigDecimal("100"));
        result.get(195).setClosePrice(new BigDecimal("85"));
        result.get(198).setClosePrice(new BigDecimal("95"));
        result.get(198).setHighPrice(new BigDecimal("95"));
        result.get(199).setClosePrice(new BigDecimal("110"));
        result.get(199).setHighPrice(new BigDecimal("110"));
        return result;
    }
}
