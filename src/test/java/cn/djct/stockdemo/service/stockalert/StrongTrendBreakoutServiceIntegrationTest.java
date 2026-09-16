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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:strong_trend;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StrongTrendBreakoutServiceIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final String STRATEGY = "STRONG_TREND_BREAKOUT";
    private final List<LocalDate> dates = new ArrayList<>();
    @Autowired private StrongTrendBreakoutService strongTrendBreakoutService;
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
        for (LocalDate day = DATE; dates.size() < 129; day = day.minusDays(1)) {
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
        when(tradeCalendarService.getPreviousTradingDay(DATE, 128)).thenReturn(dates.get(0));
        when(tradeCalendarService.getTradingDays(dates.get(0), DATE)).thenReturn(dates);
        for (int offset : List.of(3, 5, 10)) {
            when(tradeCalendarService.getPreviousTradingDay(DATE, offset)).thenReturn(dates.get(128 - offset));
        }
    }

    @Test
    void shouldSelectAllAcrossBatchesAndQueryInCodeOrder() {
        for (int i = 1; i <= 101; i++) {
            seed(String.format("%06d", i), "股票", 129);
        }
        assertEquals(101, strongTrendBreakoutService.selectStocks(DATE));
        assertEquals(101, strongTrendBreakoutService.selectStocks(DATE));
        var page = strongTrendBreakoutService.findByTradeDate(DATE, 2, 100);
        assertEquals(101, page.getTotal());
        assertEquals("000101", page.getRecords().get(0).getStockCode());
        assertEquals(new BigDecimal("10.00"), page.getRecords().get(0).getThreeDayChangePercent());
        assertEquals(new BigDecimal("10.00"), page.getRecords().get(0).getFiveDayChangePercent());
        assertEquals(new BigDecimal("10.00"), page.getRecords().get(0).getTenDayChangePercent());
        assertTrue(strongTrendBreakoutService.isCompleted(DATE));
    }

    @Test
    void shouldAllowShortHistoriesButRejectSixtyDaysGapsAndInvalidToday() {
        seed("000001", "六十八日", 68);
        seed("000002", "六十一日", 61);
        seed("000003", "六十日", 60);
        seed("000004", "中间缺失", 129);
        seed("000005", "*ST股票", 129);
        seed("000006", "当日异常", 129);
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE stock_code='000004' AND trade_date=?", dates.get(90));
        jdbcTemplate.update("UPDATE stock_daily_quote SET open_price=0,high_price=0,low_price=0 WHERE stock_code IN ('000005','000006') AND trade_date=?", DATE);
        assertEquals(2, strongTrendBreakoutService.selectStocks(DATE));
        assertEquals(List.of("000001", "000002"), strongTrendBreakoutService.findByTradeDate(DATE, 1, 20)
                .getRecords().stream().map(record -> record.getStockCode()).toList());
        jdbcTemplate.update("UPDATE stock_daily_quote SET change_percent=5 WHERE trade_date=?", DATE);
        assertEquals(0, strongTrendBreakoutService.selectStocks(DATE));
        assertTrue(strongTrendBreakoutService.isCompleted(DATE));
    }

    @Test
    void shouldIgnoreHighBeforeWindowAndFutureRecords() {
        seed("000001", "历史高点", 129);
        stockDailyQuoteMapper.upsertBatch(List.of(quote("000001", dates.get(0).minusDays(1), false)));
        jdbcTemplate.update("UPDATE stock_daily_quote SET high_price=100 WHERE trade_date < ?", dates.get(0));
        assertEquals(1, strongTrendBreakoutService.selectStocks(DATE));
        jdbcTemplate.update("UPDATE stock_daily_quote SET high_price=100 WHERE trade_date=?", dates.get(0));
        assertEquals(0, strongTrendBreakoutService.selectStocks(DATE));
    }

    @Test
    void shouldKeepOldResultsOnWriteFailureAndSeparateStrategies() {
        seed("000001", "原结果", 129);
        strongTrendBreakoutService.selectStocks(DATE);
        stockSelectionResultMapper.insertResults(DATE, "PLATFORM_BREAKOUT", List.of(stock("000001", "平台")));
        seed("000002", "新结果", 129);
        doThrow(new IllegalStateException("模拟写入失败")).when(stockSelectionResultMapper)
                .insertResults(org.mockito.ArgumentMatchers.eq(DATE), org.mockito.ArgumentMatchers.eq(STRATEGY), anyList());
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.selectStocks(DATE));
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, STRATEGY));
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, "PLATFORM_BREAKOUT"));
    }

    @Test
    void shouldValidateReadinessQueryAndCalendar() {
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.findByTradeDate(DATE, 1, 20));
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.selectStocks(DATE));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutService.selectStocks(null));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutService.selectStocks(LocalDate.now().plusDays(1)));
        assertEquals(0, strongTrendBreakoutService.selectStocks(LocalDate.of(2026, 1, 1)));
        seed("000001", "正常", 129);
        strongTrendBreakoutService.selectStocks(DATE);
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE trade_date=?", dates.get(125));
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.findByTradeDate(DATE, 1, 20));
        jdbcTemplate.update("DELETE FROM stock_daily_quote WHERE trade_date=?", DATE);
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.selectStocks(DATE));
        assertEquals(1, stockSelectionResultMapper.countResults(DATE, STRATEGY));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutService.findByTradeDate(DATE, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> strongTrendBreakoutService.findByTradeDate(DATE, 1, 101));
        when(tradeCalendarService.getTradingDays(dates.get(0), DATE)).thenReturn(dates.subList(1, 129));
        assertThrows(IllegalStateException.class, () -> strongTrendBreakoutService.selectStocks(DATE));
    }

    private StockBasic stock(String code, String name) {
        return StockBasic.builder().stockCode(code).stockName(name).lastSeenTradeDate(DATE).build();
    }

    private void seed(String code, String name, int count) {
        stockBasicMapper.upsertBatch(List.of(stock(code, name)));
        stockDailyQuoteMapper.upsertBatch(dates.subList(129 - count, 129).stream()
                .map(date -> quote(code, date, date.equals(DATE))).toList());
    }

    private StockDailyQuote quote(String code, LocalDate date, boolean today) {
        return StockDailyQuote.builder().stockCode(code).stockName("行情名称").tradeDate(date)
                .closePrice(new BigDecimal(today ? "11" : "10")).highPrice(new BigDecimal(today ? "20" : "10"))
                .openPrice(BigDecimal.TEN).lowPrice(BigDecimal.TEN).changePercent(new BigDecimal("10"))
                .turnoverAmountYuan(new BigDecimal("1000000")).dataSource("TENCENT").dataStatus("COMPLETE")
                .collectedAt(DATE.atTime(15, 2)).build();
    }
}
