package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketDailyTurnoverCalculatorTest {

    private final MarketDailyTurnoverCalculator calculator = new MarketDailyTurnoverCalculator();

    @Test
    void shouldSumShanghaiAndShenzhenTurnoverForOneTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);

        MarketDailyTurnover result = calculator.calculate(tradeDate, List.of(
                record(tradeDate, "100.25"),
                record(tradeDate, "200.75")
        ));

        assertEquals(new BigDecimal("301.00"), result.getTurnoverAmountYuan());
        assertEquals(2, result.getStockCount());
        assertEquals(2, result.getAmountRecordCount());
        assertEquals("STOCK_DAILY_QUOTE", result.getDataSource());
        assertEquals("COMPLETE", result.getDataStatus());
    }

    @Test
    void shouldRejectMissingTurnoverAmount() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);

        assertThrows(IllegalStateException.class, () -> calculator.calculate(tradeDate, List.of(
                record(tradeDate, "100.00"),
                MarketTurnoverRecordDto.builder().tradeDate(tradeDate).build()
        )));
    }

    private MarketTurnoverRecordDto record(LocalDate tradeDate, String amount) {
        return MarketTurnoverRecordDto.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(new BigDecimal(amount))
                .build();
    }
}
