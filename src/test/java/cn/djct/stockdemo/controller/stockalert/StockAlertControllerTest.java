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
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockOpenBoardAlertRespVo;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankRespVo;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankingRespVo;
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

    @Test
    void shouldReturnPlatformBreakoutWithDefaultsAndAllFields() throws Exception {
        var record = cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo.builder()
                .stockCode("000001").stockName("平台股票").openPrice(new BigDecimal("10"))
                .highPrice(new BigDecimal("12")).lowPrice(new BigDecimal("9"))
                .closePrice(new BigDecimal("11")).changePercent(new BigDecimal("10"))
                .turnoverAmountYuan(new BigDecimal("200000000"))
                .threeDayChangePercent(new BigDecimal("5"))
                .fiveDayChangePercent(new BigDecimal("8"))
                .tenDayChangePercent(new BigDecimal("10")).build();
        when(platformBreakoutService.findByTradeDate(java.time.LocalDate.of(2026, 9, 10), 1, 20))
                .thenReturn(PageRespVo.<cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo>builder()
                        .pageNum(1).pageSize(20).total(1).records(List.of(record)).build());
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "2026-09-10"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.records[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.data.records[0].openPrice").value(10))
                .andExpect(jsonPath("$.data.records[0].highPrice").value(12))
                .andExpect(jsonPath("$.data.records[0].lowPrice").value(9))
                .andExpect(jsonPath("$.data.records[0].closePrice").value(11))
                .andExpect(jsonPath("$.data.records[0].turnoverAmountYuan").value(200000000))
                .andExpect(jsonPath("$.data.records[0].threeDayChangePercent").value(5))
                .andExpect(jsonPath("$.data.records[0].fiveDayChangePercent").value(8))
                .andExpect(jsonPath("$.data.records[0].tenDayChangePercent").value(10));
        verify(platformBreakoutService).findByTradeDate(java.time.LocalDate.of(2026, 9, 10), 1, 20);
    }

    @Test
    void shouldValidatePlatformParameterBindingBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/stockAlert/platformBreakout"))
                .andExpect(jsonPath("$.message").value("请求参数不能为空：tradeDate"));
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "bad-date"))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("日期格式必须为yyyy-MM-dd：tradeDate"));
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "2026-09-10").param("pageNum", "abc"))
                .andExpect(jsonPath("$.message").value("请求参数格式错误：pageNum"));
        org.mockito.Mockito.verifyNoInteractions(platformBreakoutService);
    }

    @Test
    void shouldReportUnavailablePlatformResult() throws Exception {
        when(platformBreakoutService.findByTradeDate(java.time.LocalDate.of(2026, 9, 10), 1, 20))
                .thenThrow(new IllegalStateException("该交易日平台突破尚无成功结果"));
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "2026-09-10"))
                .andExpect(jsonPath("$.message").value("该交易日平台突破尚无成功结果"));
    }

    @Test
    void shouldForwardPlatformPaginationAndReportServiceValidation() throws Exception {
        var date = java.time.LocalDate.of(2026, 9, 10);
        when(platformBreakoutService.findByTradeDate(date, 2, 10))
                .thenReturn(PageRespVo.<cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo>builder()
                        .pageNum(2).pageSize(10).total(0).records(List.of()).build());
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "2026-09-10")
                        .param("pageNum", "2").param("pageSize", "10"))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        verify(platformBreakoutService).findByTradeDate(date, 2, 10);
        when(platformBreakoutService.findByTradeDate(date, 1, 101))
                .thenThrow(new IllegalArgumentException("页码必须大于0，每页数量必须在1到100之间"));
        mockMvc.perform(get("/api/stockAlert/platformBreakout").param("tradeDate", "2026-09-10")
                        .param("pageSize", "101"))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("页码必须大于0，每页数量必须在1到100之间"));
    }

    @MockBean
    private cn.djct.stockdemo.service.stockalert.PlatformBreakoutService platformBreakoutService;

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionResultMapper stockSelectionResultMapper;

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionRunMapper stockSelectionRunMapper;

    @Test
    void shouldReturnTurnoverRankingsForRequestedDate() throws Exception {
        var date = java.time.LocalDate.of(2026, 9, 8);
        var stocks = List.of(TechnologyStockRankRespVo.builder()
                .rank(1).stockCode("000001").stockName("科技").build());
        when(technologyStockTurnoverService.findByTradeDate(date)).thenReturn(
                cn.djct.stockdemo.pojo.vo.TechnologyStockTurnoverRespVo.builder()
                        .statisticsDate(date).increasingStocks(stocks)
                        .institutionalStocks(stocks).abnormalStocks(List.of()).build());
        mockMvc.perform(get("/api/stockAlert/technologyStockTurnover").param("tradeDate", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.statisticsDate").value("2026-09-08"))
                .andExpect(jsonPath("$.data.increasingStocks[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.data.institutionalStocks[0].rank").value(1))
                .andExpect(jsonPath("$.data.abnormalStocks").isEmpty());
        verify(technologyStockTurnoverService).findByTradeDate(date);
    }

    @Test
    void shouldReportInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/stockAlert/technologyStockTurnover").param("tradeDate", "invalid"))
                .andExpect(jsonPath("$.message").value("日期格式必须为yyyy-MM-dd：tradeDate"));
        org.mockito.Mockito.verifyNoInteractions(technologyStockTurnoverService);
    }

    @Test
    void shouldReportEmptyDateAndInvalidNumericParameter() throws Exception {
        mockMvc.perform(get("/api/stockAlert/technologyStockTurnover").param("tradeDate", ""))
                .andExpect(jsonPath("$.message").value("请求参数不能为空：tradeDate"));
        mockMvc.perform(get("/api/stockAlert/speed").param("pageNum", "invalid"))
                .andExpect(jsonPath("$.message").value("请求参数格式错误：pageNum"));
        org.mockito.Mockito.verifyNoInteractions(technologyStockTurnoverService, stockAlertService);
    }

    @Test
    void shouldReportMissingDateAndUnavailableData() throws Exception {
        mockMvc.perform(get("/api/stockAlert/technologyStockTurnover"))
                .andExpect(jsonPath("$.message").value("请求参数不能为空：tradeDate"));
        org.mockito.Mockito.verifyNoInteractions(technologyStockTurnoverService);
        var date = java.time.LocalDate.of(2026, 9, 8);
        when(technologyStockTurnoverService.findByTradeDate(date))
                .thenThrow(new IllegalStateException("该交易日日线数据尚未就绪，暂无法计算"));
        mockMvc.perform(get("/api/stockAlert/technologyStockTurnover").param("tradeDate", date.toString()))
                .andExpect(jsonPath("$.message").value("该交易日日线数据尚未就绪，暂无法计算"));
    }

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockAlertService stockAlertService;
    @MockBean
    private cn.djct.stockdemo.service.stockalert.TechnologyStockTurnoverService technologyStockTurnoverService;
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
    private cn.djct.stockdemo.mapper.StockCustomPlateMemberMapper stockCustomPlateMemberMapper;
    @MockBean
    private StockDailyQuoteMapper stockDailyQuoteMapper;
    @MockBean
    private StockFundFlowMapper stockFundFlowMapper;
    @MockBean
    private StockPlateMapper stockPlateMapper;
    @MockBean
    private cn.djct.stockdemo.mapper.StockPlateMemberMapper stockPlateMemberMapper;
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
        when(stockAlertService.findSpeedAlerts(1, 20)).thenReturn(PageRespVo.<StockSpeedAlertDto>builder()
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
                .thenReturn(PageRespVo.<StockOpenBoardAlertRespVo>builder()
                        .pageNum(2)
                        .pageSize(10)
                        .total(11)
                        .records(List.of(StockOpenBoardAlertRespVo.builder()
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
                TechnologyStockRankingRespVo.builder()
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

    private TechnologyStockRankRespVo rank(int rank, String stockCode, String stockName) {
        return TechnologyStockRankRespVo.builder()
                .rank(rank)
                .stockCode(stockCode)
                .stockName(stockName)
                .build();
    }
}
