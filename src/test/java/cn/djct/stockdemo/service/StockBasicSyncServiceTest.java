package cn.djct.stockdemo.service;

import cn.djct.stockdemo.face.StockBasicCollectionFace;
import cn.djct.stockdemo.service.impl.StockBasicSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBasicSyncServiceTest {

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private StockBasicCollectionFace stockBasicCollectionFace;

    @InjectMocks
    private StockBasicSyncServiceImpl stockBasicSyncService;

    @Test
    void shouldSkipSynchronizationOnNonTradingDay() {
        LocalDate date = LocalDate.of(2026, 8, 22);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(false);

        assertEquals(0, stockBasicSyncService.synchronize(date));

        verify(stockBasicCollectionFace, never()).synchronize(date);
    }

    @Test
    void shouldSynchronizeOnTradingDay() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);
        when(stockBasicCollectionFace.synchronize(date)).thenReturn(5200);

        assertEquals(5200, stockBasicSyncService.synchronize(date));

        verify(stockBasicCollectionFace).synchronize(date);
    }
}
