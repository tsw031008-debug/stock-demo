package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockBasicSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBasicCollectionFaceTest {

    @Mock
    private StockBasicSourceService stockBasicSourceService;

    @Mock
    private StockBasicService stockBasicService;

    @Test
    void shouldSkipNetworkRequestWhenTodayHasAlreadyBeenSynchronized() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(true);
        StockBasicCollectionFace collectionFace = createCollectionFace();

        assertEquals(0, collectionFace.synchronize(tradeDate));

        verifyNoInteractions(stockBasicSourceService);
        verify(stockBasicService, never()).saveSnapshot(tradeDate, List.of());
    }

    @Test
    void shouldSaveEveryStockReturnedByTheSource() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = createStocks(3000);
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(3000);
        when(stockBasicService.saveSnapshot(tradeDate, stocks)).thenReturn(3000);
        StockBasicCollectionFace collectionFace = createCollectionFace();

        assertEquals(3000, collectionFace.synchronize(tradeDate));

        verify(stockBasicService).saveSnapshot(tradeDate, stocks);
    }

    @Test
    void shouldRejectDuplicateStockCodes() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = createStocks(3000);
        stocks.set(2999, new StockBasicDto("000001", "重复样本"));
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(0);
        StockBasicCollectionFace collectionFace = createCollectionFace();

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(tradeDate));

        verify(stockBasicService, never()).saveSnapshot(tradeDate, stocks);
    }

    @Test
    void shouldRejectAnAbnormalDropFromTheLatestSnapshot() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = createStocks(3000);
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(4000);
        StockBasicCollectionFace collectionFace = createCollectionFace();

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(tradeDate));

        verify(stockBasicService, never()).saveSnapshot(tradeDate, stocks);
    }

    @Test
    void shouldRejectSnapshotBelowFixedMinimum() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = createStocks(2999);
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(0);
        StockBasicCollectionFace collectionFace = createCollectionFace();

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(tradeDate));

        verify(stockBasicService, never()).saveSnapshot(tradeDate, stocks);
    }

    private List<StockBasicDto> createStocks(int count) {
        List<StockBasicDto> stocks = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            String stockCode = String.format("%06d", index);
            stocks.add(new StockBasicDto(stockCode, "股票" + stockCode));
        }
        return stocks;
    }

    private StockBasicCollectionFace createCollectionFace() {
        return new StockBasicCollectionFace(
                stockBasicSourceService,
                stockBasicService
        );
    }
}
