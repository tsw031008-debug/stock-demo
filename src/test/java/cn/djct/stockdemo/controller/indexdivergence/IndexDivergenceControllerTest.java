package cn.djct.stockdemo.controller.indexdivergence;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
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
import cn.djct.stockdemo.pojo.dto.IndexDivergenceOverviewDto;
import cn.djct.stockdemo.pojo.dto.IndexDivergenceSignalDto;
import cn.djct.stockdemo.pojo.dto.IndexMinuteCurvePointDto;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndexDivergenceController.class)
@ActiveProfiles("test")
class IndexDivergenceControllerTest {

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndexDivergenceQueryService indexDivergenceQueryService;

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
    void shouldReturnLatestCurveAndSignals() throws Exception {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        LocalDateTime quoteTime = LocalDateTime.of(2026, 8, 28, 9, 31);
        when(indexDivergenceQueryService.getLatest()).thenReturn(
                IndexDivergenceOverviewDto.builder()
                        .tradeDate(tradeDate)
                        .previousClosePrice(new BigDecimal("3820.10"))
                        .curveData(List.of(IndexMinuteCurvePointDto.builder()
                                .quoteTime(quoteTime)
                                .currentPrice(new BigDecimal("3850.12"))
                                .build()))
                        .signalData(List.of(IndexDivergenceSignalDto.builder()
                                .signalType(IndexDivergenceSignalType.MACD_BOTTOM)
                                .signalTime(quoteTime.plusMinutes(20))
                                .build()))
                        .build()
        );

        mockMvc.perform(get("/api/indexDivergence/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tradeDate").value("2026-08-28"))
                .andExpect(jsonPath("$.data.previousClosePrice").value(3820.10))
                .andExpect(jsonPath("$.data.curveData[0].quoteTime")
                        .value("2026-08-28 09:31:00"))
                .andExpect(jsonPath("$.data.curveData[0].currentPrice").value(3850.12))
                .andExpect(jsonPath("$.data.signalData[0].signalType").value("MACD_BOTTOM"));
    }
}
