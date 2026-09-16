package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockSelectionResultMapper;
import cn.djct.stockdemo.mapper.StockSelectionRunMapper;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:strong_pullback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StrongStockPullbackServiceIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final String STRATEGY = "STRONG_STOCK_PULLBACK";
    private final List<LocalDate> dates = new ArrayList<>();
    @Autowired private StrongStockPullbackService strongStockPullbackService;
    @Autowired private StockBasicMapper stockBasicMapper;
    @Autowired private StockDailyQuoteMapper stockDailyQuoteMapper;
    @SpyBean private StockSelectionResultMapper stockSelectionResultMapper;
    @Autowired private StockSelectionRunMapper stockSelectionRunMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @MockBean private TradeCalendarService tradeCalendarService;

    @BeforeAll
    void schema() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V4__create_stock_tables.sql"),
                new ClassPathResource("db/migration/V17__create_stock_selection_tables.sql")).execute(dataSource);
        for (LocalDate day = DATE; dates.size() < 61; day = day.minusDays(1)) {
            if (day.getDayOfWeek().getValue() <= 5 && !day.equals(LocalDate.of(2026, 1, 1))
                    && !day.equals(LocalDate.of(2026, 1, 2))) {
                dates.add(day);
            }
        }
        Collections.reverse(dates);
    }

    @BeforeEach
    void prepare() {
        reset(stockSelectionResultMapper);
        for (String table : List.of("stock_selection_result", "stock_selection_run", "stock_daily_quote", "stock_basic")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        when(tradeCalendarService.isTradingDay(DATE)).thenReturn(true);
        when(tradeCalendarService.getPreviousTradingDay(DATE, 60)).thenReturn(dates.get(0));
        when(tradeCalendarService.getTradingDays(dates.get(0), DATE)).thenReturn(dates);
        for (int offset : List.of(3, 5, 10)) {
            when(tradeCalendarService.getPreviousTradingDay(DATE, offset)).thenReturn(dates.get(60 - offset));
        }
    }

    @Test
    void shouldRankGloballyAcrossBatchesAndReplaceOnlyCurrentStrategy() {
        for (int i = 1; i <= 101; i++) {
            seed(String.format("%06d", i), "股票", i == 101 ? "20" : "10", 0);
        }
        stockSelectionResultMapper.insertResults(DATE, "PLATFORM_BREAKOUT", List.of(stock("000099", "其他策略")));
        stockSelectionResultMapper.insertResults(dates.get(59), STRATEGY, List.of(stock("000098", "历史结果")));
        assertEquals(3, strongStockPullbackService.selectStocks(DATE));
        assertEquals(3, strongStockPullbackService.selectStocks(DATE));
        var page = strongStockPullbackService.findByTradeDate(DATE, 1, 20);
        assertEquals(List.of("000101", "000001", "000002"), page.getRecords().stream().map(r -> r.getStockCode()).toList());
        assertEquals(new BigDecimal("0.00"), page.getRecords().get(0).getTenDayChangePercent());
        assertEquals("000001", strongStockPullbackService.findByTradeDate(DATE, 2, 1).getRecords().get(0).getStockCode());
        assertTrue(strongStockPullbackService.findByTradeDate(DATE, Integer.MAX_VALUE, 100).getRecords().isEmpty());
        jdbcTemplate.update("UPDATE stock_basic SET stock_name='ST股票' WHERE stock_code='000101'");
        strongStockPullbackService.selectStocks(DATE);
        assertEquals("000001", strongStockPullbackService.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, "PLATFORM_BREAKOUT"));
        assertEquals(1, stockSelectionResultMapper.countResults(dates.get(59), STRATEGY));
    }

    @Test
    void shouldSkipIndividualMissingDataAndUseCurrentNameAndAgeEvidence() {
        seed("000001", "正常", "10", 0);
        seed("000002", "满60日", "20", 1);
        seed("000003", "缺昨收", "30", 0);
        seed("000004", "缺换手率", "40", 0);
        seed("000005", "缺历史", "50", 0);
        seed("000006", "缺今日", "60", 0);
        seed("000007", "*ST股票", "70", 0);
        seed("000008", "停牌", "80", 0);
        jdbcTemplate.update("UPDATE stock_daily_quote SET stock_name='ST历史名称' WHERE stock_code='000001'");
        jdbcTemplate.update("UPDATE stock_daily_quote SET previous_close_price=NULL WHERE stock_code='000003' AND trade_date=?", dates.get(20));
        jdbcTemplate.update("UPDATE stock_daily_quote SET turnover_rate=NULL WHERE stock_code='000004' AND trade_date=?", DATE);
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE stock_code='000005' AND trade_date=?", dates.get(20));
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE stock_code='000006' AND trade_date=?", DATE);
        jdbcTemplate.update("UPDATE stock_daily_quote SET close_price=0,turnover_amount_yuan=0 WHERE stock_code='000008' AND trade_date=?", DATE);
        assertEquals(1, strongStockPullbackService.selectStocks(DATE));
        assertEquals("000001", strongStockPullbackService.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
    }

    @Test
    void shouldDistinguishNotReadyEmptyAndPreservePreviousSuccessOnFailure() {
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.selectStocks(DATE));
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.findByTradeDate(DATE, 1, 20));
        seed("000001", "普通股票", "10", 0);
        assertEquals(1, strongStockPullbackService.selectStocks(DATE));
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE trade_date=?", DATE);
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.selectStocks(DATE));
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, STRATEGY));
        assertTrue(stockSelectionRunMapper.isCompleted(DATE, STRATEGY));
        seed("000001", "ST股票", "10", 0);
        assertEquals(0, strongStockPullbackService.selectStocks(DATE));
        assertEquals(0, strongStockPullbackService.findByTradeDate(DATE, 1, 20).getTotal());
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackService.findByTradeDate(DATE, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackService.findByTradeDate(DATE, 1, 101));
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackService.selectStocks(null));
        assertEquals(0, strongStockPullbackService.selectStocks(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void shouldRollbackDeletedResultsWhenInsertFails() {
        seed("000001", "原结果", "10", 0);
        strongStockPullbackService.selectStocks(DATE);
        seed("000002", "新增候选", "20", 0);
        doThrow(new IllegalStateException("模拟写入失败")).when(stockSelectionResultMapper)
                .insertResults(org.mockito.ArgumentMatchers.eq(DATE), org.mockito.ArgumentMatchers.eq(STRATEGY), anyList());
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.selectStocks(DATE));
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, STRATEGY));
        assertEquals("000001", strongStockPullbackService.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
    }

    @Test
    void shouldRejectInvalidCalendarAndRequiredQueryHistory() {
        seed("000001", "正常", "10", 0);
        when(tradeCalendarService.getTradingDays(dates.get(0), DATE)).thenReturn(dates.subList(1, 61));
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.selectStocks(DATE));
        when(tradeCalendarService.getTradingDays(dates.get(0), DATE)).thenReturn(dates);
        strongStockPullbackService.selectStocks(DATE);
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE trade_date=?", dates.get(57));
        assertThrows(IllegalStateException.class, () -> strongStockPullbackService.findByTradeDate(DATE, 1, 20));
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackService.findByTradeDate(LocalDate.of(2026, 1, 1), 1, 20));
        assertThrows(IllegalArgumentException.class, () -> strongStockPullbackService.selectStocks(LocalDate.now().plusDays(2)));
    }

    @Test
    void shouldCountExistingDatesAndExcludeFutureQuotes() {
        seed("000001", "六十条", "10", 1);
        var future = StockDailyQuote.builder().stockCode("000001").stockName("未来")
                .tradeDate(DATE.plusDays(1)).dataSource("TENCENT").dataStatus("COMPLETE")
                .collectedAt(DATE.atTime(15, 2)).build();
        stockDailyQuoteMapper.upsertBatch(List.of(future));
        assertEquals(60, stockDailyQuoteMapper.countByStockCodeThroughTradeDate("000001", DATE));
        assertEquals(0, strongStockPullbackService.selectStocks(DATE));
        seed("000001", "六十一条", "10", 0);
        assertEquals(61, stockDailyQuoteMapper.countByStockCodeThroughTradeDate("000001", DATE));
        assertEquals(1, strongStockPullbackService.selectStocks(DATE));
    }

    private StockBasic stock(String code, String name) {
        return StockBasic.builder().stockCode(code).stockName(name).lastSeenTradeDate(DATE).build();
    }

    private void seed(String code, String name, String turnover, int firstIndex) {
        stockBasicMapper.upsertBatch(List.of(stock(code, name)));
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = firstIndex; i <= 60; i++) {
            quotes.add(StockDailyQuote.builder().stockCode(code).stockName(name).tradeDate(dates.get(i))
                    .previousClosePrice(BigDecimal.TEN).closePrice(i >= 11 && i < 16 ? new BigDecimal("11") : BigDecimal.TEN)
                    .openPrice(BigDecimal.TEN).highPrice(new BigDecimal("12")).lowPrice(BigDecimal.TEN)
                    .turnoverRate(new BigDecimal(turnover)).turnoverAmountYuan(new BigDecimal("1000000"))
                    .changePercent(BigDecimal.ZERO).dataSource("TENCENT").dataStatus("COMPLETE")
                    .collectedAt(DATE.atTime(15, 2)).build());
        }
        stockDailyQuoteMapper.upsertBatch(quotes);
    }
}
