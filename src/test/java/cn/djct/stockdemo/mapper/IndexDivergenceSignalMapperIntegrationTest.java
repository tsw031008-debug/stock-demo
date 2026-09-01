package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
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
        "spring.datasource.url=jdbc:h2:mem:index_divergence_signal_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V9__create_index_divergence_signal.sql")
class IndexDivergenceSignalMapperIntegrationTest {

    @Autowired
    private IndexDivergenceSignalMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertByIndexCodeSignalTypeAndSignalTime() {
        IndexDivergenceSignal signal = signal("3850.00");
        mapper.upsertBatch(List.of(signal));
        signal.setCurrentPriceExtreme(new BigDecimal("3848.00"));
        mapper.upsertBatch(List.of(signal));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM index_divergence_signal",
                Integer.class
        );
        BigDecimal currentPriceExtreme = jdbcTemplate.queryForObject(
                "SELECT current_price_extreme FROM index_divergence_signal",
                BigDecimal.class
        );

        assertEquals(1, count);
        assertEquals(0, new BigDecimal("3848.00").compareTo(currentPriceExtreme));

        List<IndexDivergenceSignal> result = mapper.selectByIndexCodeAndSignalTimeRange(
                "000001",
                signal.getSignalTime().toLocalDate().atStartOfDay(),
                signal.getSignalTime().toLocalDate().plusDays(1).atStartOfDay()
        );
        assertEquals(1, result.size());
        assertEquals(IndexDivergenceSignalType.MACD_BOTTOM, result.get(0).getSignalType());
        assertEquals(signal.getSignalTime(), result.get(0).getSignalTime());
    }

    private IndexDivergenceSignal signal(String currentPriceExtreme) {
        LocalDateTime signalTime = LocalDateTime.of(2026, 8, 28, 10, 30);
        return IndexDivergenceSignal.builder()
                .indexCode("000001")
                .signalType(IndexDivergenceSignalType.MACD_BOTTOM)
                .signalTime(signalTime)
                .previousIntervalStartTime(signalTime.minusMinutes(20))
                .previousIntervalEndTime(signalTime.minusMinutes(15))
                .currentIntervalStartTime(signalTime.minusMinutes(5))
                .currentIntervalEndTime(signalTime)
                .previousPriceExtreme(new BigDecimal("3860.00"))
                .currentPriceExtreme(new BigDecimal(currentPriceExtreme))
                .previousMacdExtreme(new BigDecimal("-2.00"))
                .currentMacdExtreme(new BigDecimal("-1.00"))
                .previousDifExtreme(new BigDecimal("-1.50"))
                .currentDifExtreme(new BigDecimal("-0.80"))
                .build();
    }
}
