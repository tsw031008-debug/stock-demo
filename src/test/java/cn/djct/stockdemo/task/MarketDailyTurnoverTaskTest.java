package cn.djct.stockdemo.task;

import cn.djct.stockdemo.service.marketlevel.MarketDailyTurnoverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDailyTurnoverTaskTest {

    @Mock
    private MarketDailyTurnoverService marketDailyTurnoverService;

    @InjectMocks
    private MarketDailyTurnoverTask marketDailyTurnoverTask;

    @Test
    void shouldDelegateDailyAggregation() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 28);
        when(marketDailyTurnoverService.synchronize(tradeDate)).thenReturn(1);

        assertEquals(1, marketDailyTurnoverTask.synchronize(tradeDate, "TEST"));
    }
}
