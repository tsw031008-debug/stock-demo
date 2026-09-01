package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockFundFlowDto;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
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
        "spring.datasource.url=jdbc:h2:mem:stock_fund_flow_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/migration/V5__create_stock_fund_flow.sql")
class StockFundFlowMapperIntegrationTest {

    @Autowired
    private StockFundFlowMapper stockFundFlowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUpsertByStockCodeAndTradeDate() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 21);
        stockFundFlowMapper.upsertBatch(List.of(
                createFundFlow("600000", tradeDate, "100.00"),
                createFundFlow("000001", tradeDate, "200.00")
        ));
        stockFundFlowMapper.upsertBatch(List.of(
                createFundFlow("600000", tradeDate, "300.00")
        ));

        BigDecimal mainNetInflow = jdbcTemplate.queryForObject(
                "SELECT main_net_inflow_yuan FROM stock_fund_flow WHERE stock_code = '600000'",
                BigDecimal.class
        );
        assertEquals(2, stockFundFlowMapper.countByTradeDate(tradeDate));
        assertEquals(0, new BigDecimal("300.00").compareTo(mainNetInflow));

        List<StockFundFlowDto> page = stockFundFlowMapper.selectPageByTradeDate(
                tradeDate,
                0,
                1
        );
        assertEquals(1, page.size());
        assertEquals("600000", page.get(0).getStockCode());
        assertEquals(0, new BigDecimal("300.00").compareTo(page.get(0).getMainNetInflowYuan()));
    }

    private StockFundFlow createFundFlow(
            String stockCode,
            LocalDate tradeDate,
            String mainNetInflow
    ) {
        return StockFundFlow.builder()
                .stockCode(stockCode)
                .stockName("股票" + stockCode)
                .tradeDate(tradeDate)
                .mainNetInflowYuan(new BigDecimal(mainNetInflow))
                .dataSource("EAST_MONEY")
                .dataStatus("COMPLETE")
                .collectedAt(LocalDateTime.of(2026, 8, 21, 15, 5))
                .build();
    }
}
