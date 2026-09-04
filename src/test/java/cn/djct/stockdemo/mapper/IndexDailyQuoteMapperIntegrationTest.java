package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:index_daily_quote_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V15__create_index_daily_quote.sql")
class IndexDailyQuoteMapperIntegrationTest {

    @Autowired
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertAndQueryByTradeDates() {
        LocalDate firstDate = LocalDate.of(2026, 9, 2);
        LocalDate secondDate = LocalDate.of(2026, 9, 3);
        indexDailyQuoteMapper.upsertBatch(List.of(
                quote("000016", firstDate, "3000"),
                quote("000016", secondDate, "3010")
        ));
        indexDailyQuoteMapper.upsertBatch(List.of(quote("000016", secondDate, "3020")));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM index_daily_quote",
                Integer.class
        );
        List<IndexDailyQuote> quotes = indexDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(firstDate, secondDate),
                List.of("000016")
        );

        assertEquals(2, count);
        assertEquals(secondDate, indexDailyQuoteMapper.selectLatestTradeDate());
        assertEquals(2, quotes.size());
        assertEquals(0, new BigDecimal("3020").compareTo(quotes.get(1).getClosePrice()));
    }

    private IndexDailyQuote quote(String indexCode, LocalDate tradeDate, String closePrice) {
        return IndexDailyQuote.builder()
                .indexCode(indexCode)
                .indexName("上证50")
                .tradeDate(tradeDate)
                .quoteTime(tradeDate.atTime(15, 6))
                .closePrice(new BigDecimal(closePrice))
                .previousClosePrice(new BigDecimal("2990"))
                .dataSource("TENCENT")
                .collectedAt(LocalDateTime.of(2026, 9, 3, 15, 6))
                .build();
    }
}
