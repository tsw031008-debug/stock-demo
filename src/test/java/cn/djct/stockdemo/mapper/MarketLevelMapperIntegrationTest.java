package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:market_level_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V4__create_stock_tables.sql")
class MarketLevelMapperIntegrationTest {

    @Autowired
    private StockDailyQuoteMapper stockDailyQuoteMapper;

    @Test
    void shouldSelectRawTurnoverFieldsOnlyForShanghaiAndShenzhenAStocks() {
        LocalDate firstDate = LocalDate.of(2026, 8, 20);
        LocalDate secondDate = LocalDate.of(2026, 8, 21);
        stockDailyQuoteMapper.upsertBatch(List.of(
                quote("600000", secondDate, "100.00"),
                quote("000001", secondDate, "200.00"),
                quote("300001", secondDate, null),
                quote("430001", secondDate, "400.00"),
                quote("830001", secondDate, "500.00"),
                quote("920001", secondDate, "600.00"),
                quote("688001", firstDate, "700.00")
        ));

        List<MarketTurnoverRecordDto> result = stockDailyQuoteMapper.selectMarketTurnoverRecords(
                List.of(secondDate, firstDate)
        );

        assertEquals(4, result.size());
        assertEquals(3, result.stream().filter(record -> secondDate.equals(record.getTradeDate())).count());
        assertEquals(1, result.stream().filter(record -> firstDate.equals(record.getTradeDate())).count());
        assertEquals(new BigDecimal("1000.00"), result.stream()
                .map(MarketTurnoverRecordDto::getTurnoverAmountYuan)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertNull(result.stream()
                .filter(record -> record.getTurnoverAmountYuan() == null)
                .findFirst()
                .orElseThrow()
                .getTurnoverAmountYuan());
    }

    private StockDailyQuote quote(String stockCode, LocalDate tradeDate, String turnoverAmountYuan) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName("股票" + stockCode)
                .tradeDate(tradeDate)
                .turnoverAmountYuan(turnoverAmountYuan == null ? null : new BigDecimal(turnoverAmountYuan))
                .dataSource("TEST")
                .dataStatus("PARTIAL")
                .collectedAt(LocalDateTime.of(2026, 8, 24, 15, 2))
                .build();
    }
}
