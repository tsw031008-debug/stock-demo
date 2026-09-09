package cn.djct.stockdemo.controller.stockalert;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockCustomPlateMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.dto.TechnologyStockRankDto;
import cn.djct.stockdemo.pojo.dto.TechnologyStockRankingDto;
import cn.djct.stockdemo.service.stockalert.StockAlertService;
import cn.djct.stockdemo.service.stockalert.TechnologyStockRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockAlertController.class)
@ActiveProfiles("test")
class StockAlertControllerTest {

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockAlertService stockAlertService;
    @MockBean
    private TechnologyStockRankingService technologyStockRankingService;
    @MockBean
    private NationalHolidayMapper nationalHolidayMapper;
    @MockBean
    private TradeCalendarMapper tradeCalendarMapper;
    @MockBean
    private StockBasicMapper stockBasicMapper;
    @MockBean
    private StockCustomPlateMapper stockCustomPlateMapper;
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

    @MockBean
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;

    @Test
    void shouldReturnSpeedAlertsWithDefaultPagination() throws Exception {
        when(stockAlertService.findSpeedAlerts(1, 20)).thenReturn(PageDto.<StockSpeedAlertDto>builder()
                .pageNum(1)
                .pageSize(20)
                .total(1)
                .records(List.of(StockSpeedAlertDto.builder()
                        .stockCode("000001")
                        .stockName("平安银行")
                        .currentPrice(new BigDecimal("12.00"))
                        .currentChangePercent(new BigDecimal("8.13"))
                        .slopeDifference(new BigDecimal("2.12"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/stockAlert/speed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.data.records[0].stockName").value("平安银行"))
                .andExpect(jsonPath("$.data.records[0].currentPrice").value(12.00))
                .andExpect(jsonPath("$.data.records[0].currentChangePercent").value(8.13))
                .andExpect(jsonPath("$.data.records[0].slopeDifference").doesNotExist());
        verify(stockAlertService).findSpeedAlerts(1, 20);
    }

    @Test
    void shouldReturnOpenBoardAlertsWithRequestedPagination() throws Exception {
        when(stockAlertService.findOpenBoardAlerts(2, 10))
                .thenReturn(PageDto.<StockOpenBoardAlertDto>builder()
                        .pageNum(2)
                        .pageSize(10)
                        .total(11)
                        .records(List.of(StockOpenBoardAlertDto.builder()
                                .stockCode("600000")
                                .stockName("浦发银行")
                                .currentPrice(new BigDecimal("10.50"))
                                .currentChangePercent(new BigDecimal("10.02"))
                                .turnoverYi(new BigDecimal("8.25"))
                                .build()))
                        .build());

        mockMvc.perform(get("/api/stockAlert/openBoard")
                        .param("pageNum", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.records[0].stockCode").value("600000"))
                .andExpect(jsonPath("$.data.records[0].turnoverYi").value(8.25))
                .andExpect(jsonPath("$.data.records[0].ask1VolumeHand").doesNotExist());
    }

    @Test
    void shouldReturnTechnologyStockRankings() throws Exception {
        when(technologyStockRankingService.getLatest()).thenReturn(
                TechnologyStockRankingDto.builder()
                        .statisticsDate(java.time.LocalDate.of(2026, 9, 7))
                        .hotStocks(List.of(rank(1, "000001", "科技一")))
                        .potentialStocks(List.of(rank(1, "600000", "科技二")))
                        .build()
        );

        mockMvc.perform(get("/api/stockAlert/technologyStocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.statisticsDate").value("2026-09-07"))
                .andExpect(jsonPath("$.data.hotStocks[0].rank").value(1))
                .andExpect(jsonPath("$.data.hotStocks[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.data.hotStocks[0].stockName").value("科技一"))
                .andExpect(jsonPath("$.data.potentialStocks[0].stockCode").value("600000"));
        verify(technologyStockRankingService).getLatest();
    }

    private TechnologyStockRankDto rank(int rank, String stockCode, String stockName) {
        return TechnologyStockRankDto.builder()
                .rank(rank)
                .stockCode(stockCode)
                .stockName(stockName)
                .build();
    }
}
