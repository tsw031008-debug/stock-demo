package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndexMacdCalculatorTest {

    private static final LocalDateTime FIRST_MINUTE = LocalDateTime.of(2026, 8, 27, 9, 31);

    private final IndexMacdCalculator calculator = new IndexMacdCalculator();

    @Test
    void shouldInitializeFirstMinuteFromFirstPrice() {
        List<IndexMacdDto> result = calculator.calculate(List.of(quote(FIRST_MINUTE, "100")));

        assertEquals(1, result.size());
        assertEquals(FIRST_MINUTE, result.get(0).getQuoteTime());
        assertEquals(new BigDecimal("100"), result.get(0).getCurrentPrice());
        assertEquals(new BigDecimal("0.00000000"), result.get(0).getDif());
        assertEquals(new BigDecimal("0.00000000"), result.get(0).getDea());
        assertEquals(new BigDecimal("0.00000000"), result.get(0).getMacd());
    }

    @Test
    void shouldCalculateStandardTwelveTwentySixNineMacd() {
        List<IndexMacdDto> result = calculator.calculate(List.of(
                quote(FIRST_MINUTE, "100"),
                quote(FIRST_MINUTE.plusMinutes(1), "110")
        ));

        IndexMacdDto second = result.get(1);
        assertEquals(new BigDecimal("0.79772080"), second.getDif());
        assertEquals(new BigDecimal("0.15954416"), second.getDea());
        assertEquals(new BigDecimal("1.27635328"), second.getMacd());
    }

    @Test
    void shouldReturnZeroIndicatorsForConstantPrices() {
        List<IndexMacdDto> result = calculator.calculate(List.of(
                quote(FIRST_MINUTE, "3850.12"),
                quote(FIRST_MINUTE.plusMinutes(1), "3850.12"),
                quote(FIRST_MINUTE.plusMinutes(2), "3850.12")
        ));

        assertEquals(3, result.size());
        result.forEach(item -> {
            assertEquals(new BigDecimal("0.00000000"), item.getDif());
            assertEquals(new BigDecimal("0.00000000"), item.getDea());
            assertEquals(new BigDecimal("0.00000000"), item.getMacd());
        });
    }

    @Test
    void shouldReturnEmptyResultForEmptyQuotes() {
        assertEquals(List.of(), calculator.calculate(List.of()));
    }

    @Test
    void shouldRejectQuotesNotInAscendingMinuteOrder() {
        List<IndexMinuteQuote> quotes = List.of(
                quote(FIRST_MINUTE.plusMinutes(1), "100"),
                quote(FIRST_MINUTE, "101")
        );

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(quotes));
    }

    @Test
    void shouldRejectIncompleteOrInvalidQuote() {
        IndexMinuteQuote missingPrice = quote(FIRST_MINUTE, "100");
        missingPrice.setCurrentPrice(null);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(missingPrice)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(quote(FIRST_MINUTE, "0"))));
    }

    private IndexMinuteQuote quote(LocalDateTime quoteTime, String currentPrice) {
        return IndexMinuteQuote.builder()
                .indexCode("000001")
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal(currentPrice))
                .build();
    }
}
