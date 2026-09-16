package cn.djct.stockdemo.controller.indexstyle;

import cn.djct.stockdemo.constant.IndexStrengthType;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockCustomPlateMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.vo.IndexDailyStyleRespVo;
import cn.djct.stockdemo.pojo.vo.IndexEtfChangeRespVo;
import cn.djct.stockdemo.pojo.vo.IndexEtfComparisonRespVo;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.vo.IndexStyleItemRespVo;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
import cn.djct.stockdemo.service.indexstyle.IndexEtfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndexStyleController.class)
@ActiveProfiles("test")
class IndexStyleControllerTest {

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionResultMapper stockSelectionResultMapper;

    @MockBean
    private cn.djct.stockdemo.mapper.StockSelectionRunMapper stockSelectionRunMapper;

    @MockBean
    private cn.djct.stockdemo.service.indexstyle.StockIndexDifferenceService stockIndexDifferenceService;

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IndexStyleService indexStyleService;
    @MockBean
    private IndexEtfService indexEtfService;
    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;
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
    private cn.djct.stockdemo.mapper.StockPlateMemberMapper stockPlateMemberMapper;
    @MockBean
    private StockPlateDailyQuoteMapper stockPlateDailyQuoteMapper;
    @MockBean
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    @MockBean
    private IndexDivergenceSignalMapper indexDivergenceSignalMapper;
    @MockBean
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    @MockBean
    private StockCustomPlateMapper stockCustomPlateMapper;
    @MockBean
    private cn.djct.stockdemo.mapper.StockCustomPlateMemberMapper stockCustomPlateMemberMapper;

    @Test
    void shouldReturnFiveDayIndexStyles() throws Exception {
        LocalDate tradeDate = LocalDate.of(2026, 9, 3);
        when(indexStyleService.getLatest()).thenReturn(IndexStyleComparisonDto.builder()
                .statisticsDate(tradeDate)
                .tradeDates(List.of(
                        LocalDate.of(2026, 8, 28),
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 2),
                        tradeDate
                ))
                .indices(List.of(IndexStyleItemRespVo.builder()
                        .indexCode("000016")
                        .indexName("上证50")
                        .dailyStyles(List.of(IndexDailyStyleRespVo.builder()
                                .tradeDate(tradeDate)
                                .changePercent(new BigDecimal("1.25"))
                                .strengthType(IndexStrengthType.STRONG)
                                .build()))
                        .build()))
                .build());

        mockMvc.perform(get("/api/indexStyle/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statisticsDate").value("2026-09-03"))
                .andExpect(jsonPath("$.data.tradeDates[0]").value("2026-08-28"))
                .andExpect(jsonPath("$.data.indices[0].indexCode").value("000016"))
                .andExpect(jsonPath("$.data.indices[0].dailyStyles[0].changePercent").value(1.25))
                .andExpect(jsonPath("$.data.indices[0].dailyStyles[0].strengthType")
                        .value("STRONG"));
    }

    @Test
    void shouldReturnIndexEtfFiveDayChanges() throws Exception {
        LocalDate statisticsDate = LocalDate.of(2026, 9, 4);
        when(indexEtfService.getLatest()).thenReturn(IndexEtfComparisonRespVo.builder()
                .statisticsDate(statisticsDate)
                .baseTradeDate(LocalDate.of(2026, 8, 28))
                .etfs(List.of(IndexEtfChangeRespVo.builder()
                        .etfCode("510050")
                        .indexName("上证50")
                        .changePercent(new BigDecimal("2.35"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/indexStyle/etfChange"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statisticsDate").value("2026-09-04"))
                .andExpect(jsonPath("$.data.baseTradeDate").value("2026-08-28"))
                .andExpect(jsonPath("$.data.etfs[0].etfCode").value("510050"))
                .andExpect(jsonPath("$.data.etfs[0].changePercent").value(2.35));
    }
    @Test
    void shouldReturnDifferenceDistributionAndNullEmptyPie() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 14);
        LocalDate previous = LocalDate.of(2026, 9, 11);
        var response = new cn.djct.stockdemo.common.StockIndexDifferenceCalculator().calculate(
                date, previous, BigDecimal.ONE, BigDecimal.ONE, List.of(
                        new cn.djct.stockdemo.pojo.dto.StockClosePriceDto("000001", date, new BigDecimal("108")),
                        new cn.djct.stockdemo.pojo.dto.StockClosePriceDto("000001", previous, new BigDecimal("100"))));
        when(stockIndexDifferenceService.findByTradeDate(date)).thenReturn(response);
        mockMvc.perform(get("/api/indexStyle/stockDifference").param("tradeDate", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeDate").value("2026-09-14"))
                .andExpect(jsonPath("$.data.previousTradeDate").value("2026-09-11"))
                .andExpect(jsonPath("$.data.strong.length()").value(4))
                .andExpect(jsonPath("$.data.strong[3].count").value(1))
                .andExpect(jsonPath("$.data.strong[0].piePercent").value(25.0))
                .andExpect(jsonPath("$.data.weak[0].piePercent").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void shouldRejectMalformedDifferenceDate() throws Exception {
        mockMvc.perform(get("/api/indexStyle/stockDifference").param("tradeDate", "invalid"))
                .andExpect(jsonPath("$.message").value("日期格式必须为yyyy-MM-dd：tradeDate"));
        org.mockito.Mockito.verifyNoInteractions(stockIndexDifferenceService);
    }

    @Test
    void shouldReportMissingDifferenceDateAndMissingData() throws Exception {
        mockMvc.perform(get("/api/indexStyle/stockDifference"))
                .andExpect(jsonPath("$.message").value("请求参数不能为空：tradeDate"));
        org.mockito.Mockito.verifyNoInteractions(stockIndexDifferenceService);
        LocalDate date = LocalDate.of(2026, 9, 14);
        when(stockIndexDifferenceService.findByTradeDate(date))
                .thenThrow(new IllegalStateException("沪深300收盘行情缺失或无效：2026-09-14"));
        mockMvc.perform(get("/api/indexStyle/stockDifference").param("tradeDate", "2026-09-14"))
                .andExpect(jsonPath("$.message").value("沪深300收盘行情缺失或无效：2026-09-14"));
    }
}
