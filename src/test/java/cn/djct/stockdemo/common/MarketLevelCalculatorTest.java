package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.DailyMarketTurnoverDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketLevelCalculatorTest {

    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");

    private final MarketLevelCalculator calculator = new MarketLevelCalculator();

    @Test
    void shouldCalculateMarketLevelWithPreviousFiveTradingDays() {
        List<DailyMarketTurnoverDto> history = List.of(
                turnover(1, "7000"),
                turnover(2, "8000"),
                turnover(3, "9000"),
                turnover(4, "10000"),
                turnover(5, "11000")
        );

        MarketLevelRespVo result = calculator.calculate(amountYuan("12000"), history);

        assertEquals("过渡期", result.getStyle());
        assertEquals(new BigDecimal("8000.00"), result.getPreviousThreeDayAverageTurnoverYi());
        assertEquals(new BigDecimal("12000.00"), result.getCurrentTurnoverYi());
        assertEquals(new BigDecimal("1.33"), result.getVolumeRatio());
    }

    @ParameterizedTest
    @CsvSource({
            "7999.99,偏游资风格",
            "8000.00,过渡期",
            "10000.00,过渡期",
            "10000.01,偏机构风格"
    })
    void shouldDetermineStyleByConfirmedBoundaries(String averageYi, String expectedStyle) {
        List<DailyMarketTurnoverDto> history = List.of(
                turnover(1, averageYi),
                turnover(2, averageYi),
                turnover(3, averageYi),
                turnover(4, averageYi),
                turnover(5, averageYi)
        );

        assertEquals(expectedStyle, calculator.calculate(amountYuan("9000"), history).getStyle());
    }

    @Test
    void shouldRejectIncompleteHistoricalTurnover() {
        List<DailyMarketTurnoverDto> history = List.of(
                turnover(1, "7000"),
                turnover(2, "8000"),
                DailyMarketTurnoverDto.builder()
                        .tradeDate(LocalDate.of(2026, 8, 19))
                        .turnoverAmountYuan(amountYuan("9000"))
                        .totalRecordCount(5000)
                        .amountRecordCount(4999)
                        .build(),
                turnover(4, "10000"),
                turnover(5, "11000")
        );

        assertThrows(IllegalStateException.class,
                () -> calculator.calculate(amountYuan("12000"), history));
    }

    @Test
    void shouldRejectHistoryWithFewerThanFiveTradingDays() {
        List<DailyMarketTurnoverDto> history = List.of(
                turnover(1, "7000"),
                turnover(2, "8000"),
                turnover(3, "9000"),
                turnover(4, "10000")
        );

        assertThrows(IllegalStateException.class,
                () -> calculator.calculate(amountYuan("12000"), history));
    }

    @Test
    void shouldRejectZeroPreviousFiveDayAverage() {
        List<DailyMarketTurnoverDto> history = List.of(
                turnover(1, "0"),
                turnover(2, "0"),
                turnover(3, "0"),
                turnover(4, "0"),
                turnover(5, "0")
        );

        assertThrows(IllegalStateException.class,
                () -> calculator.calculate(amountYuan("12000"), history));
    }

    private DailyMarketTurnoverDto turnover(int dayOffset, String amountYi) {
        return DailyMarketTurnoverDto.builder()
                .tradeDate(LocalDate.of(2026, 8, 22).minusDays(dayOffset))
                .turnoverAmountYuan(amountYuan(amountYi))
                .totalRecordCount(5000)
                .amountRecordCount(5000)
                .build();
    }

    private BigDecimal amountYuan(String amountYi) {
        return new BigDecimal(amountYi).multiply(ONE_HUNDRED_MILLION);
    }
}
