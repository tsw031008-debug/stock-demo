package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockDailyQuoteService;
import cn.djct.stockdemo.service.StockDailyQuoteSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDailyQuoteCollectionFaceTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 20);

    @Mock
    private StockBasicService stockBasicService;

    @Mock
    private StockDailyQuoteSourceService stockDailyQuoteSourceService;

    @Mock
    private StockDailyQuoteService stockDailyQuoteService;

    private StockDailyQuoteCollectionFace collectionFace;

    @BeforeEach
    void setUp() {
        collectionFace = new StockDailyQuoteCollectionFace(
                stockBasicService,
                stockDailyQuoteSourceService,
                stockDailyQuoteService
        );
    }

    @Test
    void shouldCollectEveryQuoteBeforeSavingSnapshot() {
        List<StockBasic> stocks = createStocks(3000);
        List<StockDailyQuote> quotes = createQuotes(stocks);
        prepareCollection(stocks, quotes, 0);
        when(stockDailyQuoteService.saveSnapshot(quotes)).thenReturn(3000);

        assertEquals(3000, collectionFace.synchronize(TRADE_DATE));

        verify(stockDailyQuoteService).saveSnapshot(quotes);
    }

    @Test
    void shouldSkipWhenTodayIsAlreadyComplete() {
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(3000);
        when(stockDailyQuoteService.countByTradeDate(TRADE_DATE)).thenReturn(3000);

        assertEquals(0, collectionFace.synchronize(TRADE_DATE));

        verifyNoInteractions(stockDailyQuoteSourceService);
    }

    @Test
    void shouldRepairPartialSnapshotByFullIdempotentUpsert() {
        List<StockBasic> stocks = createStocks(3000);
        List<StockDailyQuote> quotes = createQuotes(stocks);
        prepareCollection(stocks, quotes, 1);
        when(stockDailyQuoteService.saveSnapshot(quotes)).thenReturn(3000);

        assertEquals(3000, collectionFace.synchronize(TRADE_DATE));
    }

    @Test
    void shouldRejectIncompleteStockBasicSnapshot() {
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(TRADE_DATE));

        verifyNoInteractions(stockDailyQuoteSourceService);
    }

    private void prepareCollection(
            List<StockBasic> stocks,
            List<StockDailyQuote> quotes,
            int savedCount
    ) {
        when(stockBasicService.countSnapshot(TRADE_DATE)).thenReturn(stocks.size());
        when(stockDailyQuoteService.countByTradeDate(TRADE_DATE)).thenReturn(savedCount);
        String lastStockCode = "";
        for (int start = 0; start < stocks.size(); start += 1000) {
            int end = Math.min(start + 1000, stocks.size());
            List<StockBasic> stockBatch = stocks.subList(start, end);
            List<StockDailyQuote> quoteBatch = quotes.subList(start, end);
            when(stockBasicService.findSnapshotBatch(TRADE_DATE, lastStockCode, 1000))
                    .thenReturn(stockBatch);
            when(stockDailyQuoteSourceService.fetchAll(TRADE_DATE, stockBatch)).thenReturn(quoteBatch);
            lastStockCode = stockBatch.get(stockBatch.size() - 1).getStockCode();
        }
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, lastStockCode, 1000)).thenReturn(List.of());
    }

    private List<StockBasic> createStocks(int count) {
        List<StockBasic> stocks = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            stocks.add(createStock(String.format("%06d", index)));
        }
        return stocks;
    }

    private List<StockDailyQuote> createQuotes(List<StockBasic> stocks) {
        return stocks.stream()
                .map(stock -> createQuote(stock.getStockCode()))
                .toList();
    }

    private StockBasic createStock(String stockCode) {
        return StockBasic.builder()
                .stockCode(stockCode)
                .stockName("股票" + stockCode)
                .lastSeenTradeDate(TRADE_DATE)
                .build();
    }

    private StockDailyQuote createQuote(String stockCode) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .tradeDate(TRADE_DATE)
                .build();
    }
}
