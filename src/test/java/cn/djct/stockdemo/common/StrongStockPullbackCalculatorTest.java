package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.util.StockMarketCodeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StrongStockPullbackCalculatorTest {
    private final StrongStockPullbackCalculator strongStockPullbackCalculator = new StrongStockPullbackCalculator();

    @ParameterizedTest
    @CsvSource({"000001,0.10", "600001,0.10", "300001,0.20", "301001,0.20", "688001,0.20", "920001,0.30", "830001,0.30", "430001,0.30"})
    void shouldUseCurrentBoardRate(String code, String rate) {
        assertEquals(new BigDecimal(rate), StockMarketCodeUtil.currentNonStLimitUpRate(code));
    }

    @Test
    void shouldRoundLimitPriceAndRequireClosingEquality() {
        StockDailyQuote quote = StockDailyQuote.builder().previousClosePrice(new BigDecimal("3.33"))
                .closePrice(new BigDecimal("3.66")).highPrice(new BigDecimal("3.66")).build();
        assertTrue(strongStockPullbackCalculator.isClosingLimitUp(quote, new BigDecimal("0.10")));
        quote.setClosePrice(new BigDecimal("3.65"));
        assertFalse(strongStockPullbackCalculator.isClosingLimitUp(quote, new BigDecimal("0.10")));
        quote.setClosePrice(new BigDecimal("3.67"));
        assertFalse(strongStockPullbackCalculator.isClosingLimitUp(quote, new BigDecimal("0.10")));
    }

    @Test
    void shouldRequireFiveLimitUpsAndStrictAmplitudeBoundary() {
        var quotes = quotes();
        assertTrue(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        quotes.get(0).setClosePrice(BigDecimal.TEN);
        assertFalse(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        quotes.get(49).setClosePrice(new BigDecimal("11"));
        assertTrue(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        quotes.get(49).setHighPrice(new BigDecimal("11.5"));
        assertFalse(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        quotes.get(49).setHighPrice(new BigDecimal("11.5001"));
        assertTrue(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        quotes.get(39).setHighPrice(new BigDecimal("100"));
        quotes.get(49).setHighPrice(new BigDecimal("11.5"));
        assertFalse(strongStockPullbackCalculator.matches(quotes, new BigDecimal("0.10")));
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackCalculator.matches(List.of(), BigDecimal.ZERO));
    }

    private List<StockDailyQuote> quotes() {
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            quotes.add(StockDailyQuote.builder().previousClosePrice(BigDecimal.TEN)
                    .closePrice(i < 5 ? new BigDecimal("11") : BigDecimal.TEN)
                    .highPrice(i == 49 ? new BigDecimal("12") : new BigDecimal("11"))
                    .lowPrice(BigDecimal.TEN).build());
        }
        return quotes;
    }
}
