package cn.djct.stockdemo.controller.indexstyle;

import cn.djct.stockdemo.constant.IndexStrengthType;
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
import cn.djct.stockdemo.pojo.dto.IndexDailyStyleDto;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.dto.IndexStyleItemDto;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
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

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IndexStyleService indexStyleService;
    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
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
    @MockBean
    private MarketDailyTurnoverMapper marketDailyTurnoverMapper;
    @MockBean
    private StockCustomPlateMapper stockCustomPlateMapper;

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
                .indices(List.of(IndexStyleItemDto.builder()
                        .indexCode("000016")
                        .indexName("上证50")
                        .dailyStyles(List.of(IndexDailyStyleDto.builder()
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
}
