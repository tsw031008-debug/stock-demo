package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.dto.*;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankRespVo;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TechnologyStockTurnoverCalculatorTest {
    private final TechnologyStockTurnoverCalculator calculator = new TechnologyStockTurnoverCalculator();
    private final List<LocalDate> dates = List.of(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 3));

    @Test
    void shouldAllowAllThreeCategoriesWithoutClosePrice() {
        var result = calculator.calculate(dates, rows("000001", "6", "2", "1.5", "1"));
        assertEquals(1, result.getIncreasingStocks().size());
        assertEquals(1, result.getInstitutionalStocks().size());
        assertEquals(1, result.getAbnormalStocks().size());
    }

    @Test
    void shouldRequireThreeComparisonsAndOnlyExcludeAffectedCategory() {
        var quotes = rows("000001", "6", "2", "1.5", "1");
        quotes.remove(3);
        var result = calculator.calculate(dates, quotes);
        assertTrue(result.getIncreasingStocks().isEmpty());
        assertEquals(1, result.getInstitutionalStocks().size());
        assertEquals(1, result.getAbnormalStocks().size());
    }

    @Test
    void shouldUseStrictAmountAndGrowthThresholds() {
        assertTrue(calculator.calculate(dates, rows("000001", "1", ".4", ".3", ".2")).getAbnormalStocks().isEmpty());
        assertTrue(calculator.calculate(dates, rows("000001", "5", "2", "1.5", "1")).getInstitutionalStocks().isEmpty());
        assertTrue(calculator.calculate(dates, rows("000001", "6", "3", "2", "1")).getAbnormalStocks().isEmpty());
        assertTrue(calculator.calculate(dates, rows("000001", "6.3", "6", "2", "1")).getInstitutionalStocks().isEmpty());
        assertTrue(calculator.calculate(dates, rows("000001", "6", "2.1", "2", "1")).getIncreasingStocks().isEmpty());
        assertTrue(calculator.calculate(dates, rows("000001", "6", "2", "1.05", "1")).getIncreasingStocks().isEmpty());
    }

    @Test
    void shouldRejectInvalidBaselineAndPublicFields() {
        for (String base : Arrays.asList(null, "0", "-1")) {
            var quotes = rows("000001", "6", base, "1.5", "1");
            var result = calculator.calculate(dates, quotes);
            assertTrue(result.getIncreasingStocks().isEmpty());
            assertTrue(result.getInstitutionalStocks().isEmpty());
            assertTrue(result.getAbnormalStocks().isEmpty());
        }
        for (String name : Arrays.asList(null, "", "ST科技", "*ST科技")) {
            var quotes = rows("000001", "6", "2", "1.5", "1");
            quotes.get(0).setStockName(name);
            assertTrue(calculator.calculate(dates, quotes).getAbnormalStocks().isEmpty());
        }
        for (BigDecimal change : Arrays.asList(null, BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            var quotes = rows("000001", "6", "2", "1.5", "1");
            quotes.get(0).setChangePercent(change);
            assertTrue(calculator.calculate(dates, quotes).getAbnormalStocks().isEmpty());
        }
        var quotes = rows("000001", null, "2", "1.5", "1");
        assertTrue(calculator.calculate(dates, quotes).getAbnormalStocks().isEmpty());
    }

    @Test
    void shouldSelectByRiseBeforeSortingByTurnover() {
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            var stock = rows("00000" + i, String.valueOf(i + 5), "2", "1.5", "1");
            stock.get(0).setChangePercent(BigDecimal.valueOf(7 - i));
            quotes.addAll(stock);
        }
        var result = calculator.calculate(dates, quotes);
        assertEquals(List.of("000005", "000004", "000003", "000002", "000001"),
                result.getAbnormalStocks().stream().map(TechnologyStockRankRespVo::getStockCode).toList());
        assertEquals(List.of(1, 2, 3, 4, 5),
                result.getAbnormalStocks().stream().map(TechnologyStockRankRespVo::getRank).toList());
    }

    @Test
    void shouldBreakTiesByCodeAndReturnEmptyForNoMatches() {
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = 6; i >= 1; i--) quotes.addAll(rows("00000" + i, "6", "2", "1.5", "1"));
        assertEquals(List.of("000001", "000002", "000003", "000004", "000005"),
                calculator.calculate(dates, quotes).getIncreasingStocks().stream()
                        .map(TechnologyStockRankRespVo::getStockCode).toList());
        assertTrue(calculator.calculate(dates, List.of()).getIncreasingStocks().isEmpty());
    }

    @Test
    void shouldRejectInvalidInputAndDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of(), List.of()));
        assertThrows(IllegalStateException.class, () -> calculator.calculate(dates, null));
        var quotes = rows("000001", "6", "2", "1.5", "1");
        quotes.add(quotes.get(0));
        assertThrows(IllegalStateException.class, () -> calculator.calculate(dates, quotes));
        quotes.remove(4);
        quotes.get(0).setTradeDate(dates.get(0).plusDays(1));
        assertThrows(IllegalStateException.class, () -> calculator.calculate(dates, quotes));
    }

    private List<StockDailyQuote> rows(String code, String... amounts) {
        List<StockDailyQuote> result = new ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            result.add(StockDailyQuote.builder().stockCode(code).stockName("科技")
                    .tradeDate(dates.get(i)).changePercent(new BigDecimal("3"))
                    .turnoverAmountYuan(amounts[i] == null ? null :
                            new BigDecimal(amounts[i]).multiply(new BigDecimal("100000000"))).build());
        }
        return result;
    }
}
