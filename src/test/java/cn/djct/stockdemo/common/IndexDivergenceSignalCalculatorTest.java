package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import cn.djct.stockdemo.pojo.dto.IndexDivergenceSignalDto;
import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexDivergenceSignalCalculatorTest {

    private static final LocalDateTime FIRST_MINUTE = LocalDateTime.of(2026, 8, 28, 9, 31);

    private final IndexDivergenceSignalCalculator calculator =
            new IndexDivergenceSignalCalculator();

    @Test
    void shouldDetectBottomDivergenceFromTwoAdjacentValidIntervals() {
        List<IndexMacdDto> macdItems = List.of(
                item(0, "110", "1", "2"),
                item(1, "100", "-5", "-10"),
                item(2, "99", "-4", "-8"),
                item(3, "98", "-3", "-7"),
                item(4, "99", "-2", "-6"),
                item(5, "100", "1", "2"),
                item(6, "97", "-3", "-6"),
                item(7, "96", "-2.5", "-5"),
                item(8, "95", "-2", "-4"),
                item(9, "96", "-1", "-2"),
                item(10, "97", "1", "2")
        );

        List<IndexDivergenceSignalDto> signals = calculator.detect(macdItems);

        assertEquals(1, signals.size());
        IndexDivergenceSignalDto signal = signals.get(0);
        assertEquals(IndexDivergenceSignalType.MACD_BOTTOM, signal.getSignalType());
        assertEquals(FIRST_MINUTE.plusMinutes(10), signal.getSignalTime());
        assertEquals(new BigDecimal("98"), signal.getPreviousPriceExtreme());
        assertEquals(new BigDecimal("95"), signal.getCurrentPriceExtreme());
        assertEquals(new BigDecimal("-10"), signal.getPreviousMacdExtreme());
        assertEquals(new BigDecimal("-6"), signal.getCurrentMacdExtreme());
        assertEquals(new BigDecimal("-5"), signal.getPreviousDifExtreme());
        assertEquals(new BigDecimal("-3"), signal.getCurrentDifExtreme());
    }

    @Test
    void shouldDetectTopDivergenceFromTwoAdjacentValidIntervals() {
        List<IndexMacdDto> macdItems = List.of(
                item(0, "100", "-1", "-2"),
                item(1, "102", "1", "2"),
                item(2, "108", "5", "10"),
                item(3, "110", "4", "8"),
                item(4, "109", "2", "4"),
                item(5, "105", "-1", "-2"),
                item(6, "108", "1", "2"),
                item(7, "112", "3", "6"),
                item(8, "115", "2.5", "5"),
                item(9, "113", "2", "4"),
                item(10, "109", "-1", "-2")
        );

        List<IndexDivergenceSignalDto> signals = calculator.detect(macdItems);

        assertEquals(1, signals.size());
        IndexDivergenceSignalDto signal = signals.get(0);
        assertEquals(IndexDivergenceSignalType.MACD_TOP, signal.getSignalType());
        assertEquals(FIRST_MINUTE.plusMinutes(10), signal.getSignalTime());
        assertEquals(new BigDecimal("110"), signal.getPreviousPriceExtreme());
        assertEquals(new BigDecimal("115"), signal.getCurrentPriceExtreme());
        assertEquals(new BigDecimal("10"), signal.getPreviousMacdExtreme());
        assertEquals(new BigDecimal("6"), signal.getCurrentMacdExtreme());
        assertEquals(new BigDecimal("5"), signal.getPreviousDifExtreme());
        assertEquals(new BigDecimal("3"), signal.getCurrentDifExtreme());
    }

    @Test
    void shouldRejectIntervalWithOnlyTwoMiddleKlines() {
        List<IndexMacdDto> macdItems = List.of(
                item(0, "110", "1", "2"),
                item(1, "100", "-5", "-10"),
                item(2, "98", "-4", "-8"),
                item(3, "99", "-2", "-4"),
                item(4, "100", "1", "2"),
                item(5, "97", "-3", "-6"),
                item(6, "96", "-2", "-4"),
                item(7, "95", "-1", "-2"),
                item(8, "97", "1", "2")
        );

        assertEquals(List.of(), calculator.detect(macdItems));
    }

    @Test
    void shouldNotTreatEqualDifAndDeaAsCross() {
        List<IndexMacdDto> macdItems = List.of(
                item(0, "100", "-1", "-2"),
                item(1, "101", "0", "0"),
                item(2, "102", "1", "2"),
                item(3, "101", "0", "0"),
                item(4, "100", "-1", "-2")
        );

        assertEquals(List.of(), calculator.detect(macdItems));
    }

    @Test
    void shouldNotDetectBottomDivergenceWhenDifExtremeIsEqual() {
        List<IndexMacdDto> macdItems = List.of(
                item(0, "110", "1", "2"),
                item(1, "100", "-5", "-10"),
                item(2, "99", "-4", "-8"),
                item(3, "98", "-3", "-7"),
                item(4, "99", "-2", "-6"),
                item(5, "100", "1", "2"),
                item(6, "97", "-5", "-6"),
                item(7, "96", "-2.5", "-5"),
                item(8, "95", "-2", "-4"),
                item(9, "96", "-1", "-2"),
                item(10, "97", "1", "2")
        );

        assertEquals(List.of(), calculator.detect(macdItems));
    }

    private IndexMacdDto item(int minuteOffset, String price, String dif, String macd) {
        return IndexMacdDto.builder()
                .quoteTime(FIRST_MINUTE.plusMinutes(minuteOffset))
                .currentPrice(new BigDecimal(price))
                .dif(new BigDecimal(dif))
                .dea(BigDecimal.ZERO)
                .macd(new BigDecimal(macd))
                .build();
    }
}
