package cn.djct.stockdemo.face;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.service.StockBasicService;
import cn.djct.stockdemo.service.StockBasicSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
        StockBasicCollectionFace collectionFace = createCollectionFace(1);

        assertEquals(0, collectionFace.synchronize(tradeDate));

        verifyNoInteractions(stockBasicSourceService);
        verify(stockBasicService, never()).saveSnapshot(tradeDate, List.of());
    }

    @Test
    void shouldSaveEveryStockReturnedByTheSource() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = List.of(
                new StockBasicDto("600000", "浦发银行"),
                new StockBasicDto("000001", "平安银行"),
                new StockBasicDto("920001", "北交样本")
        );
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(3);
        when(stockBasicService.saveSnapshot(tradeDate, stocks)).thenReturn(3);
        StockBasicCollectionFace collectionFace = createCollectionFace(1);

        assertEquals(3, collectionFace.synchronize(tradeDate));

        verify(stockBasicService).saveSnapshot(tradeDate, stocks);
    }

    @Test
    void shouldRejectDuplicateStockCodes() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = List.of(
                new StockBasicDto("600000", "浦发银行"),
                new StockBasicDto("600000", "重复样本")
        );
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(0);
        StockBasicCollectionFace collectionFace = createCollectionFace(1);

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(tradeDate));

        verify(stockBasicService, never()).saveSnapshot(tradeDate, stocks);
    }

    @Test
    void shouldRejectAnAbnormalDropFromTheLatestSnapshot() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = List.of(
                new StockBasicDto("600000", "浦发银行"),
                new StockBasicDto("000001", "平安银行")
        );
        when(stockBasicService.hasSynchronized(tradeDate)).thenReturn(false);
        when(stockBasicSourceService.fetchAll()).thenReturn(stocks);
        when(stockBasicService.countLatestSnapshot()).thenReturn(3);
        StockBasicCollectionFace collectionFace = createCollectionFace(1);

        assertThrows(IllegalStateException.class, () -> collectionFace.synchronize(tradeDate));

        verify(stockBasicService, never()).saveSnapshot(tradeDate, stocks);
    }

    private StockBasicCollectionFace createCollectionFace(int minimumStockCount) {
        return new StockBasicCollectionFace(
                stockBasicSourceService,
                stockBasicService,
                minimumStockCount
        );
    }
}
