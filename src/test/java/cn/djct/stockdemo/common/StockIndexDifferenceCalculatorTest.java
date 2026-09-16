package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceItemVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockIndexDifferenceCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 14);
    private static final LocalDate PREVIOUS = LocalDate.of(2026, 9, 11);
    private final StockIndexDifferenceCalculator stockIndexDifferenceCalculator = new StockIndexDifferenceCalculator();

    @ParameterizedTest
    @CsvSource({"101,0,0", "103,1,0", "105,2,0", "107,3,0", "108,4,0",
            "99,0,0", "97,0,1", "95,0,2", "93,0,3", "92,0,4", "100,0,0",
            "101.000000001,1,0", "98.999999999,0,1"})
    void shouldUseStrictCumulativeBoundaries(String close, int strongCount, int weakCount) {
        var result = stockIndexDifferenceCalculator.calculate(TODAY, PREVIOUS, new BigDecimal("100"),
                new BigDecimal("100"), pair("000001", close));
        for (int i = 0; i < 4; i++) {
            assertEquals(i < strongCount ? 1 : 0, result.getStrong().get(i).getCount());
            assertEquals(i < weakCount ? 1 : 0, result.getWeak().get(i).getCount());
        }
        assertEquals(1, result.getValidStockCount());
        assertEquals(0, result.getSkippedStockCount());
    }

    @Test
    void shouldCompareAgainstIndexAndUseDocumentPieDenominator() {
        List<StockClosePriceDto> quotes = new ArrayList<>(pair("000001", "109"));
        quotes.addAll(pair("900001", "105"));
        var result = stockIndexDifferenceCalculator.calculate(TODAY, PREVIOUS, new BigDecimal("101"),
                new BigDecimal("100"), quotes);
        assertEquals(List.of(2, 2, 1, 1), result.getStrong().stream().map(StockIndexDifferenceItemVo::getCount).toList());
        assertEquals(new BigDecimal("33.33"), result.getStrong().get(0).getPiePercent());
        assertEquals(new BigDecimal("16.67"), result.getStrong().get(3).getPiePercent());
        assertTrue(result.getWeak().stream().allMatch(item -> item.getPiePercent() == null));
        assertEquals(List.of(-1, -3, -5, -7), result.getWeak().stream()
                .map(StockIndexDifferenceItemVo::getThresholdPercent).toList());
    }

    @Test
    void shouldCalculateWeakPieIndependently() {
        var result = stockIndexDifferenceCalculator.calculate(TODAY, PREVIOUS, new BigDecimal("100"),
                new BigDecimal("100"), pair("000001", "92"));
        assertTrue(result.getWeak().stream().allMatch(item -> new BigDecimal("25.00").equals(item.getPiePercent())));
        assertTrue(result.getStrong().stream().allMatch(item -> item.getPiePercent() == null));
    }

    @Test
    void shouldSkipInvalidPricesAndCountOnlyCurrentCandidates() {
        List<StockClosePriceDto> quotes = new ArrayList<>(pair("good", "108"));
        quotes.addAll(pair("zero", "0"));
        quotes.add(quote("missingPrevious", TODAY, "100"));
        quotes.add(quote("null", TODAY, null));
        quotes.add(quote("oldOnly", PREVIOUS, "100"));
        quotes.add(quote("zeroPrevious", TODAY, "100"));
        quotes.add(quote("zeroPrevious", PREVIOUS, "0"));
        var result = stockIndexDifferenceCalculator.calculate(TODAY, PREVIOUS, BigDecimal.ONE, BigDecimal.ONE, quotes);
        assertEquals(5, result.getTotalStockCount());
        assertEquals(1, result.getValidStockCount());
        assertEquals(4, result.getSkippedStockCount());
    }

    @Test
    void shouldRejectMissingEntireDayAndInvalidIndex() {
        assertThrows(IllegalStateException.class, () -> stockIndexDifferenceCalculator.calculate(
                TODAY, PREVIOUS, BigDecimal.ONE, BigDecimal.ONE, List.of()));
        assertThrows(IllegalStateException.class, () -> stockIndexDifferenceCalculator.calculate(
                TODAY, PREVIOUS, BigDecimal.ONE, BigDecimal.ONE, List.of(quote("s", TODAY, "100"))));
        assertThrows(IllegalStateException.class, () -> stockIndexDifferenceCalculator.calculate(
                TODAY, PREVIOUS, BigDecimal.ZERO, BigDecimal.ONE, pair("s", "100")));
        assertThrows(IllegalStateException.class, () -> stockIndexDifferenceCalculator.calculate(
                TODAY, PREVIOUS, BigDecimal.ONE, null, pair("s", "100")));
    }

    private List<StockClosePriceDto> pair(String code, String close) {
        return List.of(quote(code, TODAY, close), quote(code, PREVIOUS, "100"));
    }

    private StockClosePriceDto quote(String code, LocalDate date, String price) {
        return new StockClosePriceDto(code, date, price == null ? null : new BigDecimal(price));
    }
}
