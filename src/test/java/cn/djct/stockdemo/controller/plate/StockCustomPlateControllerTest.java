package cn.djct.stockdemo.controller.plate;

import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
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
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverComparisonDto;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverItemDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;
import cn.djct.stockdemo.service.plate.StockCategoryTurnoverComparisonService;
import cn.djct.stockdemo.service.plate.StockCustomPlateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockCustomPlateController.class)
@ActiveProfiles("test")
class StockCustomPlateControllerTest {

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockCustomPlateService stockCustomPlateService;
    @MockBean
    private StockCategoryTurnoverComparisonService comparisonService;
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
    private StockCustomPlateMapper stockCustomPlateMapper;
    @MockBean
    private IndexMinuteQuoteMapper indexMinuteQuoteMapper;
    @MockBean
    private IndexDivergenceSignalMapper indexDivergenceSignalMapper;
    @MockBean
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;

    @Test
    void shouldReturnTwoTradingDayTurnoversInYi() throws Exception {
        when(comparisonService.getCurrent()).thenReturn(
                StockCategoryTurnoverComparisonDto.builder()
                        .statisticsDate(LocalDate.of(2026, 8, 28))
                        .previousTradeDate(LocalDate.of(2026, 8, 27))
                        .categories(List.of(StockCategoryTurnoverItemDto.builder()
                                .categoryCode("TECHNOLOGY")
                                .categoryName("科技")
                                .stockCount(523)
                                .currentTurnoverYi(new BigDecimal("3250.10"))
                                .previousTurnoverYi(new BigDecimal("3010.20"))
                                .build()))
                        .build()
        );

        mockMvc.perform(get("/api/plate/turnoverComparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statisticsDate").value("2026-08-28"))
                .andExpect(jsonPath("$.data.previousTradeDate").value("2026-08-27"))
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("科技"))
                .andExpect(jsonPath("$.data.categories[0].stockCount").value(523))
                .andExpect(jsonPath("$.data.categories[0].currentTurnoverYi").value(3250.10));
    }

    @Test
    void shouldCreateCustomPlateWithCompleteMemberList() throws Exception {
        when(stockCustomPlateService.create(any(StockCustomPlateSaveDto.class)))
                .thenReturn(StockCustomPlateDto.builder()
                .id(16L)
                .categoryCode("TECHNOLOGY")
                .categoryName("科技")
                .plateName("人工智能")
                .build());

        mockMvc.perform(post("/api/plate/customPlates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryCode": "TECHNOLOGY",
                                  "plateName": "人工智能",
                                  "stockCodes": ["000001", "600000"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(16))
                .andExpect(jsonPath("$.data.categoryName").value("科技"))
                .andExpect(jsonPath("$.data.plateName").value("人工智能"));
    }
}
