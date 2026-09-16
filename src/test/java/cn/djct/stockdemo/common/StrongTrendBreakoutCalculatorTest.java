package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StrongTrendBreakoutCalculatorTest {
    private final StrongTrendBreakoutCalculator strongTrendBreakoutCalculator = new StrongTrendBreakoutCalculator();

    @Test
    void shouldUseAvailableHistoryAndExcludeTodayHigh() {
        for (int size : List.of(61, 68, 128, 129)) {
            var quotes = quotes(size);
            assertTrue(strongTrendBreakoutCalculator.matches(quotes));
            quotes.get(0).setHighPrice(new BigDecimal("12"));
            assertFalse(strongTrendBreakoutCalculator.matches(quotes));
        }
    }

    @Test
    void shouldRequireStrictPriceAndGainBoundaries() {
        var quotes = quotes(129);
        var today = quotes.get(128);
        today.setClosePrice(BigDecimal.TEN);
        assertFalse(strongTrendBreakoutCalculator.matches(quotes));
        today.setClosePrice(new BigDecimal("10.01"));
        today.setChangePercent(new BigDecimal("5"));
        assertFalse(strongTrendBreakoutCalculator.matches(quotes));
        today.setChangePercent(new BigDecimal("5.0001"));
        assertTrue(strongTrendBreakoutCalculator.matches(quotes));
        quotes.get(127).setHighPrice(new BigDecimal("10.01"));
        assertFalse(strongTrendBreakoutCalculator.matches(quotes));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutCalculator.matches(quotes(60)));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutCalculator.matches(quotes(130)));
    }

    private List<StockDailyQuote> quotes(int size) {
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            quotes.add(StockDailyQuote.builder().highPrice(BigDecimal.TEN).closePrice(BigDecimal.TEN).build());
        }
        var today = quotes.get(size - 1);
        today.setClosePrice(new BigDecimal("11"));
        today.setHighPrice(new BigDecimal("20"));
        today.setChangePercent(new BigDecimal("10"));
        return quotes;
    }
}
