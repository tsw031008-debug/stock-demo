package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:index_etf_daily_quote;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V16__create_index_etf_daily_quote.sql")
class IndexEtfDailyQuoteMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Test
    void shouldCreateTableAndRejectDuplicateEtfTradeDate() {
        insertQuote(jdbcTemplate);
        LocalDate tradeDate = LocalDate.of(2026, 9, 3);
        assertEquals(3, indexEtfDailyQuoteMapper.upsertBatch(List.of(
                quote("510300", "沪深300", tradeDate, "4.2100"),
                quote("159949", "创业板50", tradeDate, "1.3500"),
                quote("512100", "中证1000", tradeDate, "2.6100")
        )));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM index_etf_daily_quote",
                Integer.class
        );

        assertEquals(4, count);
        List<String> etfCodes = List.of("510050", "510300", "159949", "512100");
        assertEquals(tradeDate, indexEtfDailyQuoteMapper.selectLatestTradeDate());
        assertEquals(4, indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(tradeDate),
                etfCodes
        ).size());
        assertThrows(DataIntegrityViolationException.class, () -> insertQuote(jdbcTemplate));
    }

    private void insertQuote(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                """
                        INSERT INTO index_etf_daily_quote (
                            etf_code, index_name, trade_date, quote_time,
                            close_price, data_source, collected_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                "510050",
                "上证50",
                LocalDate.of(2026, 9, 3),
                Timestamp.valueOf(LocalDateTime.of(2026, 9, 3, 15, 7)),
                "3.2150",
                "TENCENT",
                Timestamp.valueOf(LocalDateTime.of(2026, 9, 3, 15, 7))
        );
    }

    private IndexEtfDailyQuote quote(
            String etfCode,
            String indexName,
            LocalDate tradeDate,
            String closePrice
    ) {
        return IndexEtfDailyQuote.builder()
                .etfCode(etfCode)
                .indexName(indexName)
                .tradeDate(tradeDate)
                .quoteTime(LocalDateTime.of(tradeDate, java.time.LocalTime.of(15, 7)))
                .closePrice(new BigDecimal(closePrice))
                .dataSource("TENCENT")
                .collectedAt(LocalDateTime.of(tradeDate, java.time.LocalTime.of(15, 7)))
                .build();
    }
}
