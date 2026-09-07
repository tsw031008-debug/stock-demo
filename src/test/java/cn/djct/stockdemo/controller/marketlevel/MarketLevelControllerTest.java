package cn.djct.stockdemo.controller.marketlevel;

import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockCustomPlateMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.dto.MarketLevelDto;
import cn.djct.stockdemo.pojo.dto.MarketPeriodComparisonDto;
import cn.djct.stockdemo.pojo.dto.MarketPeriodItemDto;
import cn.djct.stockdemo.service.marketlevel.MarketLevelService;
import cn.djct.stockdemo.service.marketlevel.MarketPeriodComparisonService;
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

@WebMvcTest(MarketLevelController.class)
@ActiveProfiles("test")
class MarketLevelControllerTest {

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @MockBean
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketLevelService marketLevelService;

    @MockBean
    private MarketPeriodComparisonService marketPeriodComparisonService;

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
    void shouldReturnLatestMarketLevel() throws Exception {
        when(marketLevelService.getLatest()).thenReturn(MarketLevelDto.builder()
                .style("过渡期")
                .previousThreeDayAverageTurnoverYi(new BigDecimal("8000.00"))
                .currentTurnoverYi(new BigDecimal("9000.00"))
                .volumeRatio(new BigDecimal("1.06"))
                .build());

        mockMvc.perform(get("/api/marketLevel/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.style").value("过渡期"))
                .andExpect(jsonPath("$.data.previousThreeDayAverageTurnoverYi").value(8000.00))
                .andExpect(jsonPath("$.data.currentTurnoverYi").value(9000.00))
                .andExpect(jsonPath("$.data.volumeRatio").value(1.06))
                .andExpect(jsonPath("$.data.statisticsTradeDate").doesNotExist());
    }

    @Test
    void shouldReturnWeeklyAndMonthlyPeriodComparison() throws Exception {
        LocalDate statisticsDate = LocalDate.of(2026, 8, 28);
        when(marketPeriodComparisonService.getLatest()).thenReturn(MarketPeriodComparisonDto.builder()
                .statisticsTradeDate(statisticsDate)
                .weekly(List.of(item("YOY", "2026-07-20~2026-07-24", "9000.00")))
                .monthly(List.of(item("CURRENT", "2026年08月", "10000.00")))
                .build());

        mockMvc.perform(get("/api/marketLevel/periodComparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statisticsTradeDate").value("2026-08-28"))
                .andExpect(jsonPath("$.data.weekly[0].comparisonType").value("YOY"))
                .andExpect(jsonPath("$.data.weekly[0].averageTurnoverYi").value(9000.00))
                .andExpect(jsonPath("$.data.monthly[0].periodLabel").value("2026年08月"));
    }

    private MarketPeriodItemDto item(String type, String label, String average) {
        return MarketPeriodItemDto.builder()
                .comparisonType(type)
                .periodLabel(label)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 28))
                .tradingDayCount(20)
                .averageTurnoverYi(new BigDecimal(average))
                .available(true)
                .build();
    }
}
