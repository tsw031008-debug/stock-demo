package cn.djct.stockdemo.controller.stockfundflow;

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
import cn.djct.stockdemo.pojo.vo.StockFundFlowRespVo;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowService;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.ZoneId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockFundFlowController.class)
@ActiveProfiles("test")
class StockFundFlowControllerTest {

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionResultMapper stockSelectionResultMapper;

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionRunMapper stockSelectionRunMapper;

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockFundFlowSyncService stockFundFlowSyncService;

    @MockBean
    private StockFundFlowService stockFundFlowService;

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
    void shouldSynchronizeCurrentStockFundFlowSnapshot() throws Exception {
        LocalDate tradeDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(stockFundFlowSyncService.synchronize(tradeDate)).thenReturn(5200);

        mockMvc.perform(post("/api/stockFundFlow/synchronize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.tradeDate").value(tradeDate.toString()))
                .andExpect(jsonPath("$.data.savedCount").value(5200));
    }

    @Test
    void shouldQueryFundFlowsByTradeDate() throws Exception {
        LocalDate tradeDate = LocalDate.of(2026, 8, 25);
        StockFundFlowRespVo record = StockFundFlowRespVo.builder()
                .tradeDate(tradeDate)
                .stockCode("600519")
                .stockName("贵州茅台")
                .latestPrice(new BigDecimal("1304.00"))
                .changePercent(new BigDecimal("-0.05"))
                .mainNetInflowYuan(new BigDecimal("-199480352.00"))
                .mainNetInflowRatio(new BigDecimal("-7.23"))
                .superLargeNetInflowYuan(new BigDecimal("-58395136.00"))
                .superLargeNetInflowRatio(new BigDecimal("-2.12"))
                .largeNetInflowYuan(new BigDecimal("-141085216.00"))
                .largeNetInflowRatio(new BigDecimal("-5.12"))
                .build();
        when(stockFundFlowService.findByTradeDate(tradeDate, 1, 20))
                .thenReturn(PageRespVo.<StockFundFlowRespVo>builder()
                        .pageNum(1)
                        .pageSize(20)
                        .total(5211)
                        .records(List.of(record))
                        .build());

        mockMvc.perform(get("/api/stockFundFlow")
                        .param("tradeDate", tradeDate.toString())
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(5211))
                .andExpect(jsonPath("$.data.records[0].tradeDate").value("2026-08-25"))
                .andExpect(jsonPath("$.data.records[0].stockCode").value("600519"))
                .andExpect(jsonPath("$.data.records[0].stockName").value("贵州茅台"))
                .andExpect(jsonPath("$.data.records[0].mainNetInflowYuan").value(-199480352.00))
                .andExpect(jsonPath("$.data.records[0].mainNetInflowRatio").value(-7.23));
    }
}
