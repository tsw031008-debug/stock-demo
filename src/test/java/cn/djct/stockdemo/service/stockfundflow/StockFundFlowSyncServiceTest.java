package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.face.StockFundFlowCollectionFace;
import cn.djct.stockdemo.service.stockfundflow.impl.StockFundFlowSyncServiceImpl;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
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
class StockFundFlowSyncServiceTest {

    @Mock
    private TradeCalendarService tradeCalendarService;

    @Mock
    private StockFundFlowCollectionFace stockFundFlowCollectionFace;

    @InjectMocks
    private StockFundFlowSyncServiceImpl stockFundFlowSyncService;

    @Test
    void shouldSkipNonTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 22);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(false);

        assertEquals(0, stockFundFlowSyncService.synchronize(tradeDate));

        verify(stockFundFlowCollectionFace, never()).synchronize(tradeDate);
    }

    @Test
    void shouldSynchronizeTradingDay() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 21);
        when(tradeCalendarService.isTradingDay(tradeDate)).thenReturn(true);
        when(stockFundFlowCollectionFace.synchronize(tradeDate)).thenReturn(5200);

        assertEquals(5200, stockFundFlowSyncService.synchronize(tradeDate));
    }
}
