package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.mapper.StockSelectionRunMapper;
import cn.djct.stockdemo.mapper.StockSelectionResultMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:platform_breakout;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlatformBreakoutServiceIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String STRATEGY = "PLATFORM_BREAKOUT";
    private final List<LocalDate> dates = tradingDates();
    @Autowired private PlatformBreakoutService service;
    @Autowired private StockSelectionResultMapper resultMapper;
    @Autowired private StockSelectionRunMapper runMapper;
    @Autowired private StockBasicMapper basicMapper;
    @Autowired private StockDailyQuoteMapper quoteMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @MockBean private TradeCalendarService calendar;

    @BeforeAll
    void schema() {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V4__create_stock_tables.sql"),
                new ClassPathResource("db/migration/V17__create_stock_selection_tables.sql")).execute(dataSource);
    }

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM stock_selection_result");
        jdbc.update("DELETE FROM stock_selection_run");
        jdbc.update("DELETE FROM stock_basic");
        jdbc.update("DELETE FROM stock_daily_quote");
        when(calendar.isTradingDay(DATE)).thenReturn(true);
        when(calendar.getPreviousTradingDay(DATE, 199)).thenReturn(dates.get(0));
        when(calendar.getTradingDays(dates.get(0), DATE)).thenReturn(dates);
        for (int offset : List.of(3, 5, 10)) {
            when(calendar.getPreviousTradingDay(DATE, offset)).thenReturn(dates.get(199 - offset));
        }
    }

    @Test
    void shouldSucceedAfterRolledBackFailure() {
        assertThrows(IllegalStateException.class, () -> service.selectStocks(DATE));
        seed("000001", "重试选股");
        assertEquals(1, service.selectStocks(DATE));
        assertEquals(1, resultMapper.countResults(DATE, STRATEGY));
        assertTrue(runMapper.isCompleted(DATE, STRATEGY));
    }

    @Test
    void shouldSaveQueryAndReplaceResultsWithoutDuplicatesOrStaleMembers() {
        seed("600001", "甲");
        seed("000001", "乙");
        seed("300001", "丙");
        assertEquals(3, service.selectStocks(DATE));
        assertEquals(3, service.selectStocks(DATE));
        var page = service.findByTradeDate(DATE, 1, 1);
        assertEquals(3, page.getTotal());
        assertEquals("000001", page.getRecords().get(0).getStockCode());
        var record = page.getRecords().get(0);
        assertEquals("乙", record.getStockName());
        assertEquals(0, new BigDecimal("110").compareTo(record.getClosePrice()));
        assertEquals(0, new BigDecimal("22.22").compareTo(record.getThreeDayChangePercent()));
        assertEquals(0, new BigDecimal("22.22").compareTo(record.getFiveDayChangePercent()));
        assertEquals(0, new BigDecimal("22.22").compareTo(record.getTenDayChangePercent()));
        assertEquals(0, new BigDecimal("200000000").compareTo(record.getTurnoverAmountYuan()));
        assertEquals("300001", service.findByTradeDate(DATE, 2, 1).getRecords().get(0).getStockCode());
        assertTrue(service.findByTradeDate(DATE, Integer.MAX_VALUE, 100).getRecords().isEmpty());
        jdbc.update("UPDATE stock_basic SET stock_name = 'ST甲' WHERE stock_code = '600001'");
        assertEquals(2, service.selectStocks(DATE));
        assertEquals(2, resultMapper.countResults(DATE, STRATEGY));
        assertThrows(DuplicateKeyException.class, () -> resultMapper.insertResults(DATE, STRATEGY,
                List.of(StockBasic.builder().stockCode("000001").stockName("重复").build())));
    }

    @Test
    void shouldKeepHistoricalNameAndRequestedDateAfterBasicSnapshotChanges() {
        seed("000001", "入选时名称");
        service.selectStocks(DATE);
        jdbc.update("UPDATE stock_basic SET stock_name = '新名称', last_seen_trade_date = ?", DATE.plusDays(1));
        var later = quote("000001", DATE.plusDays(1), 199);
        later.setClosePrice(new BigDecimal("120"));
        quoteMapper.upsertBatch(List.of(later));
        var result = service.findByTradeDate(DATE, 1, 20).getRecords().get(0);
        assertEquals("入选时名称", result.getStockName());
        assertEquals(0, new BigDecimal("110").compareTo(result.getClosePrice()));
    }

    @Test
    void shouldDistinguishSuccessfulEmptyResultFromNoSuccessfulRun() {
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(DATE, 1, 20));
        seed("000001", "ST股票");
        assertEquals(0, service.selectStocks(DATE));
        assertTrue(runMapper.isCompleted(DATE, STRATEGY));
        assertEquals(0, service.findByTradeDate(DATE, 1, 20).getTotal());
        assertTrue(service.findByTradeDate(DATE, 1, 20).getRecords().isEmpty());
    }

    @Test
    void shouldFilterOnlyStPrefixesAndBothZeroSuspension() {
        seed("000001", "*ST股票");
        seed("000002", "ST股票");
        seed("000003", "股票ST");
        seed("000004", "停牌");
        jdbc.update("UPDATE stock_daily_quote SET close_price=0, turnover_amount_yuan=0 WHERE stock_code='000004' AND trade_date=?", DATE);
        jdbc.update("UPDATE stock_daily_quote SET turnover_amount_yuan=0 WHERE stock_code='000003' AND trade_date=?", DATE);
        assertEquals(1, service.selectStocks(DATE));
        assertEquals("000003", service.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
    }

    @Test
    void shouldContinueSelectingHealthyStocksWhenHistoryHasGapsOrInvalidPrices() {
        seed("000001", "完整行情");
        seed("000002", "历史不足");
        jdbc.update("DELETE FROM stock_daily_quote WHERE stock_code='000002' AND trade_date < ?", dates.get(10));
        assertEquals(1, service.selectStocks(DATE));
        jdbc.update("DELETE FROM stock_daily_quote WHERE stock_code='000002' AND trade_date = ?", dates.get(20));
        assertEquals(1, service.selectStocks(DATE));
        quoteMapper.upsertBatch(List.of(quote("000002", dates.get(20), 20)));
        quoteMapper.upsertBatch(List.of(quote("000002", dates.get(0).minusDays(3), 0)));
        assertEquals(1, service.selectStocks(DATE));
        seed("000002", "历史价格异常");
        jdbc.update("UPDATE stock_daily_quote SET high_price=NULL WHERE stock_code='000002' AND trade_date=?", dates.get(20));
        assertEquals(1, service.selectStocks(DATE));
        assertTrue(runMapper.isCompleted(DATE, STRATEGY));
        assertEquals("000001", service.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
        assertEquals(1, resultMapper.countResults(DATE, STRATEGY));
        // 修复后能够重新入选，重算仍清理上次结果。
        quoteMapper.upsertBatch(List.of(quote("000002", dates.get(20), 20)));
        assertEquals(2, service.selectStocks(DATE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARTIAL", "MISSING", "ZERO_PRICE", "NULL_HIGH", "NULL_CHANGE", "NEGATIVE_AMOUNT"})
    void shouldRejectIncompleteTodayWithoutSavingResults(String kind) {
        seed("000001", "甲");
        switch (kind) {
            case "PARTIAL" -> jdbc.update("UPDATE stock_daily_quote SET data_status='PARTIAL' WHERE trade_date=?", DATE);
            case "MISSING" -> jdbc.update("DELETE FROM stock_daily_quote WHERE trade_date=?", DATE);
            case "ZERO_PRICE" -> jdbc.update("UPDATE stock_daily_quote SET close_price=0 WHERE trade_date=?", DATE);
            case "NULL_HIGH" -> jdbc.update("UPDATE stock_daily_quote SET high_price=NULL WHERE trade_date=?", DATE);
            case "NULL_CHANGE" -> jdbc.update("UPDATE stock_daily_quote SET change_percent=NULL WHERE trade_date=?", DATE);
            case "NEGATIVE_AMOUNT" -> jdbc.update("UPDATE stock_daily_quote SET turnover_amount_yuan=-1 WHERE trade_date=?", DATE);
        }
        assertThrows(IllegalStateException.class, () -> service.selectStocks(DATE));
        assertFalse(runMapper.isCompleted(DATE, STRATEGY));
        assertEquals(0, resultMapper.countResults(DATE, STRATEGY));
    }

    @Test
    void shouldRollBackDeletedResultsAndNewBatchWhenLaterBatchFails() {
        // 101只股票跨越100只的批次边界，先写入一批后第二批失败必须整体回滚。
        List<StockBasic> stocks = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            String code = String.format("%06d", i);
            stocks.add(StockBasic.builder().stockCode(code).stockName("ST过滤").lastSeenTradeDate(DATE).build());
            quoteMapper.upsertBatch(List.of(quote(code, DATE, 199)));
        }
        basicMapper.upsertBatch(stocks);
        seed("000000", "旧结果");
        assertEquals(1, service.selectStocks(DATE));
        seed("000001", "新增结果");
        jdbc.update("DELETE FROM stock_daily_quote WHERE stock_code='000100'");
        assertThrows(IllegalStateException.class, () -> service.selectStocks(DATE));
        assertTrue(runMapper.isCompleted(DATE, STRATEGY));
        assertEquals(1, resultMapper.countResults(DATE, STRATEGY));
        assertEquals("000000", service.findByTradeDate(DATE, 1, 20).getRecords().get(0).getStockCode());
    }

    @Test
    void shouldRejectMissingHistoryDuringQueryInsteadOfReturningNullGain() {
        seed("000001", "甲");
        service.selectStocks(DATE);
        jdbc.update("DELETE FROM stock_daily_quote WHERE trade_date=?", dates.get(196));
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(DATE, 1, 20));
        jdbc.update("DELETE FROM stock_daily_quote WHERE trade_date=?", DATE);
        assertThrows(IllegalStateException.class, () -> service.findByTradeDate(DATE, 1, 20));
    }

    @Test
    void shouldValidateDatesPaginationCalendarAndSnapshot() {
        assertThrows(IllegalArgumentException.class, () -> service.selectStocks(null));
        assertThrows(IllegalArgumentException.class, () -> service.selectStocks(LocalDate.of(2099, 1, 1)));
        assertEquals(0, service.selectStocks(LocalDate.of(2026, 9, 5)));
        assertEquals(0, service.selectStocks(LocalDate.of(2026, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(DATE, 1, 101));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(DATE, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(DATE, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.findByTradeDate(LocalDate.of(2026, 1, 1), 1, 20));
        assertThrows(IllegalStateException.class, () -> service.selectStocks(DATE));
        seed("000001", "甲");
        when(calendar.getTradingDays(dates.get(0), DATE)).thenReturn(dates.subList(1, 200));
        assertThrows(IllegalStateException.class, () -> service.selectStocks(DATE));
    }

    private void seed(String code, String name) {
        basicMapper.upsertBatch(List.of(StockBasic.builder().stockCode(code).stockName(name).lastSeenTradeDate(DATE).build()));
        List<StockDailyQuote> quotes = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            quotes.add(quote(code, dates.get(i), i));
        }
        quoteMapper.upsertBatch(quotes);
    }

    private StockDailyQuote quote(String code, LocalDate date, int index) {
        BigDecimal close = new BigDecimal(index >= 170 ? "90" : "80");
        BigDecimal high = close.add(BigDecimal.ONE);
        if (index == 174) high = new BigDecimal("100");
        if (index == 195) close = new BigDecimal("85");
        if (index == 198) { close = new BigDecimal("95"); high = close; }
        if (index == 199) { close = new BigDecimal("110"); high = close; }
        return StockDailyQuote.builder().stockCode(code).stockName("行情名称").tradeDate(date)
                .closePrice(close).highPrice(high).openPrice(close).lowPrice(close)
                .changePercent(new BigDecimal("15.7895")).turnoverAmountYuan(new BigDecimal("200000000"))
                .dataSource("TENCENT").dataStatus("COMPLETE").collectedAt(LocalDateTime.of(2026, 9, 10, 15, 2)).build();
    }

    private static List<LocalDate> tradingDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = DATE;
        while (dates.size() < 200) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !date.equals(LocalDate.of(2026, 1, 1)) && !date.equals(LocalDate.of(2026, 5, 1))) {
                dates.add(date);
            }
            date = date.minusDays(1);
        }
        Collections.reverse(dates);
        return dates;
    }
}
