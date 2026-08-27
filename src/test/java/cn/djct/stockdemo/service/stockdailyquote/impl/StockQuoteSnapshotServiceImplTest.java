package cn.djct.stockdemo.service.stockdailyquote.impl;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.stockbasic.StockBasicService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockQuoteSnapshotServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 26);

    @Mock
    private StockBasicService stockBasicService;
    @Mock
    private StockDailyQuoteSourceService stockDailyQuoteSourceService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldReuseSnapshotWithinSixtySeconds() {
        List<StockBasic> stocks = stocks(3000);
        List<StockDailyQuote> quotes = quotes(3000);
        configureStocks(stocks);
        when(stockDailyQuoteSourceService.fetchAll(TRADE_DATE, stocks)).thenReturn(quotes);
        StockQuoteSnapshotServiceImpl service = createService();

        List<StockDailyQuote> first = service.getSnapshot(TRADE_DATE);
        List<StockDailyQuote> second = service.getSnapshot(TRADE_DATE);

        assertSame(first, second);
        assertEquals(3000, first.size());
        verify(stockDailyQuoteSourceService).fetchAll(TRADE_DATE, stocks);
    }

    @Test
    void shouldRejectIncompleteStockListBeforeRequestingQuotes() {
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(2999);
        StockQuoteSnapshotServiceImpl service = createService();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.getSnapshot(TRADE_DATE)
        );

        assertEquals("当天股票清单不完整，minimum=3000，actual=2999", exception.getMessage());
        verifyNoInteractions(stockDailyQuoteSourceService);
    }

    /**
     * 创建固定时钟的快照服务。
     */
    private StockQuoteSnapshotServiceImpl createService() {
        return new StockQuoteSnapshotServiceImpl(
                stockBasicService,
                stockDailyQuoteSourceService,
                Clock.fixed(Instant.parse("2026-08-26T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    /**
     * 配置三批股票清单游标读取。
     */
    private void configureStocks(List<StockBasic> stocks) {
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(3000);
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "", 1000))
                .thenReturn(stocks.subList(0, 1000));
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "000999", 1000))
                .thenReturn(stocks.subList(1000, 2000));
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, "001999", 1000))
                .thenReturn(stocks.subList(2000, 3000));
    }

    /**
     * 创建指定数量的股票清单。
     */
    private List<StockBasic> stocks(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> StockBasic.builder()
                        .stockCode(String.format("%06d", index))
                        .stockName("股票" + index)
                        .build())
                .toList();
    }

    /**
     * 创建指定数量的股票实时行情。
     */
    private List<StockDailyQuote> quotes(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> StockDailyQuote.builder()
                        .stockCode(String.format("%06d", index))
                        .build())
                .toList();
    }
}
