package cn.djct.stockdemo.controller.stockdailyquote;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
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
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockDailyQuoteController.class)
@ActiveProfiles("test")
class StockDailyQuoteControllerTest {

    @MockBean
    private IndexDailyQuoteMapper indexDailyQuoteMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockDailyQuoteSyncService stockDailyQuoteSyncService;

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
    void shouldSynchronizeCurrentStockDailyQuoteSnapshot() throws Exception {
        LocalDate tradeDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(stockDailyQuoteSyncService.synchronize(tradeDate)).thenReturn(5547);

        mockMvc.perform(post("/api/stockDailyQuote/synchronize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.tradeDate").value(tradeDate.toString()))
                .andExpect(jsonPath("$.data.savedCount").value(5547));
    }
}
