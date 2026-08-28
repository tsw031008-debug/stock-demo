package cn.djct.stockdemo.controller.plate;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockPlateLimitUpDto;
import cn.djct.stockdemo.service.plate.StockPlateDailyQuoteService;
import cn.djct.stockdemo.service.plate.StockPlateQueryService;
import cn.djct.stockdemo.service.plate.StockPlateSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockPlateController.class)
@ActiveProfiles("test")
class StockPlateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockPlateQueryService stockPlateQueryService;
    @MockBean
    private StockPlateSyncService stockPlateSyncService;
    @MockBean
    private StockPlateDailyQuoteService stockPlateDailyQuoteService;
    @MockBean
    private NationalHolidayMapper nationalHolidayMapper;
    @MockBean
    private TradeCalendarMapper tradeCalendarMapper;
    @MockBean
    private StockBasicMapper stockBasicMapper;
    @MockBean
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @MockBean
    private StockFundFlowMapper stockFundFlowMapper;
    @MockBean
    private StockPlateMapper stockPlateMapper;
    @MockBean
    private StockPlateDailyQuoteMapper stockPlateDailyQuoteMapper;
    @MockBean
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    @MockBean
    private IndexDivergenceSignalMapper indexDivergenceSignalMapper;

    @Test
    void shouldReturnLimitUpStatisticsWithDefaultPagination() throws Exception {
        when(stockPlateQueryService.findLimitUpStatistics(1, 20)).thenReturn(
                PageDto.<StockPlateLimitUpDto>builder()
                        .pageNum(1)
                        .pageSize(20)
                        .total(1)
                        .records(List.of(StockPlateLimitUpDto.builder()
                                .plateName("科技-概")
                                .totalStockCount(100)
                                .plateChangePercent(new BigDecimal("1.25"))
                                .limitUpStockCount(5)
                                .limitUpRatio(new BigDecimal("5.00"))
                                .build()))
                        .build()
        );

        mockMvc.perform(get("/api/plate/limitUpStatistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].plateName").value("科技-概"))
                .andExpect(jsonPath("$.data.records[0].totalStockCount").value(100))
                .andExpect(jsonPath("$.data.records[0].plateChangePercent").value(1.25))
                .andExpect(jsonPath("$.data.records[0].limitUpStockCount").value(5))
                .andExpect(jsonPath("$.data.records[0].limitUpRatio").value(5.00));
    }

    @Test
    void shouldNotExposeHistoricalBackfillEndpoint() throws Exception {
        mockMvc.perform(post("/api/plate/backfillDailyQuotes")
                        .param("endDate", "2026-08-26"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(stockPlateDailyQuoteService);
    }

    @Test
    void shouldSynchronizeCurrentPlateDailyQuote() throws Exception {
        when(stockPlateDailyQuoteService.synchronize(org.mockito.ArgumentMatchers.any()))
                .thenReturn(200);

        mockMvc.perform(post("/api/plate/synchronizeDailyQuote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedCount").value(200));
    }
}
