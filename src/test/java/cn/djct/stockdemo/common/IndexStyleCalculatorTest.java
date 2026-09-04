package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexStrengthType;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.pojo.dto.IndexStyleItemDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndexStyleCalculatorTest {

    private final IndexStyleCalculator calculator = new IndexStyleCalculator();

    @Test
    void shouldMarkDailyStrongestAndWeakestInFixedIndexOrder() {
        List<LocalDate> tradeDates = tradeDates();
        List<IndexDailyQuote> quotes = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < tradeDates.size(); dayIndex++) {
            quotes.add(quote(IndexStyleIndex.SSE_50, tradeDates.get(dayIndex), 101 + dayIndex));
            quotes.add(quote(IndexStyleIndex.SHANGHAI_COMPOSITE,
                    tradeDates.get(dayIndex), 102 + dayIndex));
            quotes.add(quote(IndexStyleIndex.CHINEXT_COMPOSITE,
                    tradeDates.get(dayIndex), 99 - dayIndex));
            quotes.add(quote(IndexStyleIndex.CSI_1000, tradeDates.get(dayIndex), 100));
        }

        List<IndexStyleItemDto> result = calculator.calculate(tradeDates, quotes);

        assertEquals(List.of("000016", "000001", "399102", "000852"),
                result.stream().map(IndexStyleItemDto::getIndexCode).toList());
        assertEquals(IndexStrengthType.STRONG,
                result.get(1).getDailyStyles().get(0).getStrengthType());
        assertEquals(IndexStrengthType.WEAK,
                result.get(2).getDailyStyles().get(0).getStrengthType());
        assertEquals(new BigDecimal("2.00"),
                result.get(1).getDailyStyles().get(0).getChangePercent());
    }

    @Test
    void shouldLeaveAllIndicesUnmarkedWhenDailyChangesAreEqual() {
        List<LocalDate> tradeDates = tradeDates();
        List<IndexDailyQuote> quotes = new ArrayList<>();
        for (LocalDate tradeDate : tradeDates) {
            for (IndexStyleIndex index : IndexStyleIndex.values()) {
                quotes.add(quote(index, tradeDate, 100));
            }
        }

        List<IndexStyleItemDto> result = calculator.calculate(tradeDates, quotes);

        assertEquals(20, result.stream().mapToInt(item -> item.getDailyStyles().size()).sum());
        result.forEach(item -> item.getDailyStyles().forEach(style ->
                assertEquals(IndexStrengthType.NONE, style.getStrengthType())));
    }

    @Test
    void shouldRejectMissingIndexQuote() {
        List<LocalDate> tradeDates = tradeDates();
        List<IndexDailyQuote> quotes = new ArrayList<>();
        for (LocalDate tradeDate : tradeDates) {
            for (IndexStyleIndex index : IndexStyleIndex.values()) {
                quotes.add(quote(index, tradeDate, 100));
            }
        }
        quotes.remove(0);

        assertThrows(IllegalStateException.class,
                () -> calculator.calculate(tradeDates, quotes));
    }

    private List<LocalDate> tradeDates() {
        return List.of(
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3)
        );
    }

    private IndexDailyQuote quote(IndexStyleIndex index, LocalDate tradeDate, int closePrice) {
        return IndexDailyQuote.builder()
                .indexCode(index.getIndexCode())
                .indexName(index.getIndexName())
                .tradeDate(tradeDate)
                .closePrice(BigDecimal.valueOf(closePrice))
                .previousClosePrice(new BigDecimal("100"))
                .build();
    }
}
