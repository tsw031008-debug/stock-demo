package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:index_minute_quote_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V8__create_index_minute_quote.sql")
class IndexMinuteQuoteMapperIntegrationTest {

    @Autowired
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertByIndexCodeAndQuoteTime() {
        LocalDateTime firstMinute = LocalDateTime.of(2026, 8, 27, 9, 31);
        indexMinuteQuoteMapper.upsert(quote(firstMinute, "3850.12"));
        indexMinuteQuoteMapper.upsert(quote(firstMinute, "3850.20"));
        indexMinuteQuoteMapper.upsert(quote(firstMinute.plusMinutes(1), "3851.10"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM index_minute_quote",
                Integer.class
        );
        BigDecimal updatedPrice = jdbcTemplate.queryForObject(
                "SELECT current_price FROM index_minute_quote WHERE quote_time = ?",
                BigDecimal.class,
                firstMinute
        );

        assertEquals(2, count);
        assertEquals(0, new BigDecimal("3850.20").compareTo(updatedPrice));

        List<IndexMinuteQuote> result = indexMinuteQuoteMapper
                .selectByIndexCodeAndQuoteTimeRange(
                        "000001",
                        firstMinute,
                        firstMinute.plusMinutes(1)
                );

        assertEquals(2, result.size());
        assertEquals(firstMinute, result.get(0).getQuoteTime());
        assertEquals(firstMinute.plusMinutes(1), result.get(1).getQuoteTime());
    }

    private IndexMinuteQuote quote(LocalDateTime quoteTime, String currentPrice) {
        return IndexMinuteQuote.builder()
                .indexCode("000001")
                .indexName("上证指数")
                .tradeDate(quoteTime.toLocalDate())
                .quoteTime(quoteTime)
                .currentPrice(new BigDecimal(currentPrice))
                .previousClosePrice(new BigDecimal("3820.10"))
                .dataSource("TENCENT")
                .collectedAt(quoteTime.plusSeconds(10))
                .build();
    }
}
