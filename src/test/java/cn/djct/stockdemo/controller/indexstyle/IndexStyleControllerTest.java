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
}
