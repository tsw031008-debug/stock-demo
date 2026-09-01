package cn.djct.stockdemo.controller.tradecalendar;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.IndexDivergenceSignalMapper;
import cn.djct.stockdemo.mapper.IndexMinuteQuoteMapper;
import cn.djct.stockdemo.mapper.MarketDailyTurnoverMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.StockPlateDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeCalendarController.class)
@ActiveProfiles("test")
class TradeCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeCalendarService tradeCalendarService;

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

    @Test
    void shouldReturnTradingDayResult() throws Exception {
        LocalDate date = LocalDate.of(2023, 6, 26);
        when(tradeCalendarService.isTradingDay(date)).thenReturn(true);

        mockMvc.perform(
                        get("/api/tradeCalendar/tradingDay")
                                .param("date", "2023-06-26")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.date").value("2023-06-26"))
                .andExpect(jsonPath("$.data.tradingDay").value(true));
    }

    @Test
    void shouldReturnPreviousTradingDay() throws Exception {
        LocalDate date = LocalDate.of(2023, 6, 26);
        LocalDate previousTradingDay = LocalDate.of(2023, 6, 20);
        when(tradeCalendarService.getPreviousTradingDay(date, 2)).thenReturn(previousTradingDay);

        mockMvc.perform(
                        get("/api/tradeCalendar/previousTradingDay")
                                .param("date", "2023-06-26")
                                .param("offset", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.baseDate").value("2023-06-26"))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.tradingDate").value("2023-06-20"));
    }

    @Test
    void shouldReturnNextTradingDay() throws Exception {
        LocalDate date = LocalDate.of(2023, 6, 26);
        LocalDate nextTradingDay = LocalDate.of(2023, 6, 28);
        when(tradeCalendarService.getNextTradingDay(date, 2)).thenReturn(nextTradingDay);

        mockMvc.perform(
                        get("/api/tradeCalendar/nextTradingDay")
                                .param("date", "2023-06-26")
                                .param("offset", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.baseDate").value("2023-06-26"))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.tradingDate").value("2023-06-28"));
    }

    @Test
    void shouldInitializeTradeCalendar() throws Exception {
        LocalDate startDate = LocalDate.of(2017, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        when(tradeCalendarService.initialize(startDate, endDate)).thenReturn(3652);

        mockMvc.perform(
                        post("/api/tradeCalendar/initialize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "startDate": "2017-01-01",
                                          "endDate": "2026-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.startDate").value("2017-01-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-12-31"))
                .andExpect(jsonPath("$.data.insertedCount").value(3652));
    }

    @Test
    void shouldRejectMissingStartDate() throws Exception {
        mockMvc.perform(
                        post("/api/tradeCalendar/initialize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "endDate": "2026-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("开始日期不能为空"));

        verifyNoInteractions(tradeCalendarService);
    }

    @Test
    void shouldRejectReversedDateRange() throws Exception {
        mockMvc.perform(
                        post("/api/tradeCalendar/initialize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "startDate": "2026-12-31",
                                          "endDate": "2017-01-01"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("开始日期不能晚于结束日期"));

        verifyNoInteractions(tradeCalendarService);
    }

    @Test
    void shouldRejectMissingRequestBody() throws Exception {
        mockMvc.perform(
                        post("/api/tradeCalendar/initialize")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("请求体不能为空或格式错误"));

        verifyNoInteractions(tradeCalendarService);
    }

    @Test
    void shouldReturnConflictWhenAlreadyInitialized() throws Exception {
        LocalDate startDate = LocalDate.of(2017, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        when(tradeCalendarService.initialize(startDate, endDate))
                .thenThrow(new IllegalStateException("指定日期范围已经存在交易日历"));

        mockMvc.perform(
                        post("/api/tradeCalendar/initialize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "startDate": "2017-01-01",
                                          "endDate": "2026-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("指定日期范围已经存在交易日历"));
    }
}
