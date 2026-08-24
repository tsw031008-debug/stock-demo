package cn.djct.stockdemo.controller;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.service.StockFundFlowSyncService;
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

@WebMvcTest(StockFundFlowController.class)
@ActiveProfiles("test")
class StockFundFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockFundFlowSyncService stockFundFlowSyncService;

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
}
