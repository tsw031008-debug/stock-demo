package cn.djct.stockdemo.service.stockbasic;

import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.pojo.dto.StockBasicDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.service.stockbasic.impl.StockBasicServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockBasicServiceTest {

    @Mock
    private StockBasicMapper stockBasicMapper;

    @InjectMocks
    private StockBasicServiceImpl stockBasicService;

    @Test
    void shouldUpsertStocksInBatchesOfFiveHundred() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 20);
        List<StockBasicDto> stocks = new ArrayList<>();
        for (int index = 0; index < 1001; index++) {
            stocks.add(new StockBasicDto(String.format("%06d", index), "股票" + index));
        }

        assertEquals(1001, stockBasicService.saveSnapshot(tradeDate, stocks));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockBasic>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockBasicMapper, times(3)).upsertBatch(batchCaptor.capture());
        assertEquals(List.of(500, 500, 1), batchCaptor.getAllValues().stream().map(List::size).toList());
        assertEquals(tradeDate, batchCaptor.getAllValues().get(0).get(0).getLastSeenTradeDate());
    }
}
