package cn.djct.stockdemo.controller.marketlevel;

import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.dto.MarketLevelDto;
import cn.djct.stockdemo.service.marketlevel.MarketLevelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketLevelController.class)
@ActiveProfiles("test")
class MarketLevelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketLevelService marketLevelService;

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
}
