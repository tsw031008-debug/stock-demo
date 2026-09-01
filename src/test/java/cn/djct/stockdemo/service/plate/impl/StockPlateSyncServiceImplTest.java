package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
import cn.djct.stockdemo.service.plate.StockPlateService;
import cn.djct.stockdemo.service.plate.StockPlateSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockPlateSyncServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 26);

    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private StockPlateSourceService stockPlateSourceService;
    @Mock
    private StockPlateService stockPlateService;

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
    void shouldSkipSourceRequestOnNonTradingDay() {
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(false);
        StockPlateSyncServiceImpl service = createService();

        assertEquals(0, service.synchronize(TRADE_DATE));
        verifyNoInteractions(stockPlateSourceService, stockPlateService);
    }

    @Test
    void shouldFetchThenSaveCompleteSnapshot() {
        List<StockPlateSourceDto> plates = List.of(StockPlateSourceDto.builder()
                .plateName("科技-概")
                .stockCodes(List.of("000001"))
                .build());
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(stockPlateService.hasSynchronized(TRADE_DATE)).thenReturn(false);
        when(stockPlateSourceService.fetchAll()).thenReturn(plates);
        when(stockPlateService.saveSnapshot(TRADE_DATE, plates)).thenReturn(1);
        StockPlateSyncServiceImpl service = createService();

        assertEquals(1, service.synchronize(TRADE_DATE));
    }

    /**
     * 创建板块同步服务。
     */
    private StockPlateSyncServiceImpl createService() {
        return new StockPlateSyncServiceImpl(
                tradeCalendarService,
                stockPlateSourceService,
                stockPlateService
        );
    }
}
