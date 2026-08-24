package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockFundFlowService;
import cn.djct.stockdemo.service.StockFundFlowSourceService;
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
class StockFundFlowCollectionFaceTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 21);

    @Mock
    private StockBasicService stockBasicService;

    @Mock
    private StockFundFlowSourceService stockFundFlowSourceService;

    @Mock
    private StockFundFlowService stockFundFlowService;

    private StockFundFlowCollectionFace collectionFace;

    @BeforeEach
    void setUp() {
        collectionFace = new StockFundFlowCollectionFace(
                stockBasicService,
                stockFundFlowSourceService,
                stockFundFlowService
        );
    }

    @Test
    void shouldCollectShanghaiAndShenzhenStocksBeforeSaving() {
        List<StockBasic> stocks = createStocks(3000);
        List<StockFundFlow> fundFlows = createFundFlows(stocks);
        prepareStockBatches(stocks);
        when(stockFundFlowService.countByTradeDate(TRADE_DATE)).thenReturn(0);
        when(stockFundFlowSourceService.fetchAll(TRADE_DATE, stocks)).thenReturn(fundFlows);
        when(stockFundFlowService.saveSnapshot(fundFlows)).thenReturn(3000);

        assertEquals(3000, collectionFace.synchronize(TRADE_DATE));

        verify(stockFundFlowService).saveSnapshot(fundFlows);
    }

    @Test
    void shouldExcludeBeijingStocks() {
        List<StockBasic> stocks = createStocks(3000);
        stocks.add(createStock("920001"));
        List<StockFundFlow> fundFlows = createFundFlows(stocks.subList(0, 3000));
        prepareStockBatches(stocks);
        when(stockFundFlowService.countByTradeDate(TRADE_DATE)).thenReturn(0);
        when(stockFundFlowSourceService.fetchAll(TRADE_DATE, stocks.subList(0, 3000)))
                .thenReturn(fundFlows);
        when(stockFundFlowService.saveSnapshot(fundFlows)).thenReturn(3000);

        assertEquals(3000, collectionFace.synchronize(TRADE_DATE));
    }

    @Test
    void shouldSkipWhenSnapshotIsComplete() {
        List<StockBasic> stocks = createStocks(3000);
        prepareStockBatches(stocks);
        when(stockFundFlowService.countByTradeDate(TRADE_DATE)).thenReturn(3000);

        assertEquals(0, collectionFace.synchronize(TRADE_DATE));

        verifyNoInteractions(stockFundFlowSourceService);
    }

    @Test
    void shouldRejectStockSnapshotBelowFixedMinimum() {
        List<StockBasic> stocks = createStocks(2999);
        prepareStockBatches(stocks);

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(TRADE_DATE));

        verifyNoInteractions(stockFundFlowSourceService);
    }

    private void prepareStockBatches(List<StockBasic> stocks) {
        String lastStockCode = "";
        for (int start = 0; start < stocks.size(); start += 1000) {
            int end = Math.min(start + 1000, stocks.size());
            List<StockBasic> batch = stocks.subList(start, end);
            when(stockBasicService.findSnapshotBatch(TRADE_DATE, lastStockCode, 1000))
                    .thenReturn(batch);
            lastStockCode = batch.get(batch.size() - 1).getStockCode();
        }
        when(stockBasicService.findSnapshotBatch(TRADE_DATE, lastStockCode, 1000))
                .thenReturn(List.of());
    }

    private List<StockBasic> createStocks(int count) {
        List<StockBasic> stocks = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            stocks.add(createStock(String.format("%06d", index)));
        }
        return stocks;
    }

    private List<StockFundFlow> createFundFlows(List<StockBasic> stocks) {
        return stocks.stream()
                .map(stock -> StockFundFlow.builder()
                        .stockCode(stock.getStockCode())
                        .stockName(stock.getStockName())
                        .tradeDate(TRADE_DATE)
                        .build())
                .toList();
    }

    private StockBasic createStock(String stockCode) {
        return StockBasic.builder()
                .stockCode(stockCode)
                .stockName("股票" + stockCode)
                .lastSeenTradeDate(TRADE_DATE)
                .build();
    }
}
