package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
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

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:market_daily_turnover_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V10__create_market_daily_turnover.sql")
class MarketDailyTurnoverMapperIntegrationTest {

    @Autowired
    private MarketDailyTurnoverMapper mapper;

    @Test
    void shouldUpsertAndQueryCompleteRecordsByRange() {
        LocalDate firstDate = LocalDate.of(2026, 8, 27);
        LocalDate secondDate = LocalDate.of(2026, 8, 28);
        mapper.upsert(turnover(firstDate, "100.00"));
        mapper.upsert(turnover(secondDate, "200.00"));
        mapper.upsert(turnover(secondDate, "300.00"));

        List<MarketDailyTurnover> result = mapper.selectCompleteByDateRange(firstDate, secondDate);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("300.00"), result.get(1).getTurnoverAmountYuan());
        assertEquals(secondDate, mapper.selectLatestCompleteTradeDate());
    }

    private MarketDailyTurnover turnover(LocalDate tradeDate, String amount) {
        return MarketDailyTurnover.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(new BigDecimal(amount))
                .stockCount(2)
                .amountRecordCount(2)
                .dataSource("TEST")
                .dataStatus("COMPLETE")
                .calculatedAt(LocalDateTime.of(2026, 8, 28, 15, 4))
                .build();
    }
}
