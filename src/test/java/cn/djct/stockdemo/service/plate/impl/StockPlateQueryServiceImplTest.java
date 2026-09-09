package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.common.StockPlateCalculator;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockPlateLimitUpRespVo;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.plate.StockPlateService;
import cn.djct.stockdemo.service.stockdailyquote.StockQuoteSnapshotService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockPlateQueryServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 26);

    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private StockPlateService stockPlateService;
    @Mock
    private StockQuoteSnapshotService stockQuoteSnapshotService;
    @Mock
    private StockPlateCalculator stockPlateCalculator;

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
    void shouldRejectNonTradingDayBeforeReadingPlateData() {
        StockPlateQueryServiceImpl service = createService("2026-08-26T02:00:00Z");
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findLimitUpStatistics(1, 20)
        );

        assertEquals("当前为非交易日", exception.getMessage());
        verifyNoInteractions(stockPlateService, stockQuoteSnapshotService, stockPlateCalculator);
    }

    @Test
    void shouldCalculateThenPaginateStatistics() {
        StockPlateQueryServiceImpl service = createService("2026-08-26T02:00:00Z");
        List<StockPlateMemberDto> members = List.of(StockPlateMemberDto.builder()
                .plateId(1L)
                .plateName("科技-概")
                .stockCode("000001")
                .build());
        List<StockDailyQuote> quotes = List.of(StockDailyQuote.builder()
                .stockCode("000001")
                .build());
        List<StockPlateLimitUpRespVo> statistics = List.of(
                statistic("板块一"),
                statistic("板块二"),
                statistic("板块三")
        );
        when(tradeCalendarService.isTradingDay(TRADE_DATE)).thenReturn(true);
        when(stockPlateService.findActiveMembers()).thenReturn(members);
        when(stockQuoteSnapshotService.getSnapshot(TRADE_DATE)).thenReturn(quotes);
        when(stockPlateCalculator.calculateLimitUpStatistics(members, quotes)).thenReturn(statistics);

        PageRespVo<StockPlateLimitUpRespVo> result = service.findLimitUpStatistics(2, 2);

        assertEquals(3, result.getTotal());
        assertEquals(List.of("板块三"), result.getRecords().stream()
                .map(StockPlateLimitUpRespVo::getPlateName)
                .toList());
    }

    @Test
    void shouldRejectInvalidPageSizeBeforeTradingDayCheck() {
        StockPlateQueryServiceImpl service = createService("2026-08-26T02:00:00Z");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findLimitUpStatistics(1, 101)
        );

        assertEquals("每页数量必须在1到100之间", exception.getMessage());
        verifyNoInteractions(tradeCalendarService);
    }

    /**
     * 创建固定时钟的板块查询服务。
     */
    private StockPlateQueryServiceImpl createService(String instant) {
        return new StockPlateQueryServiceImpl(
                tradeCalendarService,
                stockPlateService,
                stockQuoteSnapshotService,
                stockPlateCalculator,
                Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai"))
        );
    }

    /**
     * 创建板块统计结果。
     */
    private StockPlateLimitUpRespVo statistic(String plateName) {
        return StockPlateLimitUpRespVo.builder()
                .plateName(plateName)
                .build();
    }
}
