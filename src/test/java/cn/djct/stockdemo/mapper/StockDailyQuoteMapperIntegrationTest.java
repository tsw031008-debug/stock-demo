package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
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
        "spring.datasource.url=jdbc:h2:mem:stock_daily_quote_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V4__create_stock_tables.sql")
class StockDailyQuoteMapperIntegrationTest {

    @Autowired
    private StockDailyQuoteMapper stockDailyQuoteMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertQuoteByStockCodeAndTradeDate() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        stockDailyQuoteMapper.upsertBatch(List.of(
                createQuote("600000", "浦发银行", tradeDate, "10.00"),
                createQuote("000001", "平安银行", tradeDate, "12.00")
        ));

        stockDailyQuoteMapper.upsertBatch(List.of(
                createQuote("600000", "浦发银行", tradeDate, "10.50")
        ));

        BigDecimal closePrice = jdbcTemplate.queryForObject(
                "SELECT close_price FROM stock_daily_quote WHERE stock_code = '600000'",
                BigDecimal.class
        );
        assertEquals(2, stockDailyQuoteMapper.countByTradeDate(tradeDate));
        assertEquals(0, new BigDecimal("10.50").compareTo(closePrice));
    }

    private StockDailyQuote createQuote(
            String stockCode,
            String stockName,
            LocalDate tradeDate,
            String closePrice
    ) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .dataSource("TENCENT")
                .dataStatus("COMPLETE")
                .collectedAt(LocalDateTime.of(2026, 8, 20, 15, 3))
                .build();
    }
}
