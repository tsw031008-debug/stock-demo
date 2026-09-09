package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.pojo.dto.IndexEtfChangeDto;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndexEtfChangeCalculatorTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 8, 28);
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 9, 4);

    private final IndexEtfChangeCalculator calculator = new IndexEtfChangeCalculator();

    @Test
    void shouldCalculateFiveDayChangesInFixedOrder() {
        List<IndexEtfDailyQuote> quotes = new ArrayList<>();
        for (int index = 0; index < IndexEtf.values().length; index++) {
            IndexEtf etf = IndexEtf.values()[index];
            quotes.add(quote(etf, BASE_DATE, "100"));
            quotes.add(quote(etf, STATISTICS_DATE, String.valueOf(102 + index)));
        }

        List<IndexEtfChangeDto> result = calculator.calculate(
                BASE_DATE,
                STATISTICS_DATE,
                quotes
        );

        assertEquals(List.of("510050", "510300", "159949", "512100"),
                result.stream().map(IndexEtfChangeDto::getEtfCode).toList());
        assertEquals(new BigDecimal("2.00"), result.get(0).getChangePercent());
        assertEquals(new BigDecimal("5.00"), result.get(3).getChangePercent());
    }

    @Test
    void shouldRejectMissingBaseQuote() {
        List<IndexEtfDailyQuote> quotes = new ArrayList<>();
        for (IndexEtf etf : IndexEtf.values()) {
            quotes.add(quote(etf, BASE_DATE, "100"));
            quotes.add(quote(etf, STATISTICS_DATE, "101"));
        }
        quotes.remove(0);

        assertThrows(
                IllegalStateException.class,
                () -> calculator.calculate(BASE_DATE, STATISTICS_DATE, quotes)
        );
    }

    private IndexEtfDailyQuote quote(IndexEtf etf, LocalDate tradeDate, String closePrice) {
        return IndexEtfDailyQuote.builder()
                .etfCode(etf.getEtfCode())
                .indexName(etf.getIndexName())
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .build();
    }
}
