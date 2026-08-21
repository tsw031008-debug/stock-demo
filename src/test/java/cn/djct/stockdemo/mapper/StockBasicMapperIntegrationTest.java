package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V4__create_stock_tables.sql")
class StockBasicMapperIntegrationTest {

    @Autowired
    private StockBasicMapper stockBasicMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpdateExistingStockWithoutCreatingDuplicateRows() {
        LocalDate firstTradeDate = LocalDate.of(2026, 8, 19);
        LocalDate secondTradeDate = LocalDate.of(2026, 8, 20);
        stockBasicMapper.upsertBatch(List.of(
                createStock("600000", "旧名称", firstTradeDate),
                createStock("000001", "平安银行", firstTradeDate)
        ));

        stockBasicMapper.upsertBatch(List.of(
                createStock("600000", "浦发银行", secondTradeDate),
                createStock("920001", "北交样本", secondTradeDate)
        ));

        Integer totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_basic", Integer.class);
        String updatedName = jdbcTemplate.queryForObject(
                "SELECT stock_name FROM stock_basic WHERE stock_code = '600000'",
                String.class
        );
        assertEquals(3, totalCount);
        assertEquals(2, stockBasicMapper.countByLastSeenTradeDate(secondTradeDate));
        assertEquals("浦发银行", updatedName);
        assertEquals(List.of("600000", "920001"),
                stockBasicMapper.selectSnapshotAfterCode(secondTradeDate, "", 200)
                        .stream()
                        .map(StockBasic::getStockCode)
                        .toList());
    }

    private StockBasic createStock(String stockCode, String stockName, LocalDate tradeDate) {
        return StockBasic.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .lastSeenTradeDate(tradeDate)
                .build();
    }
}
