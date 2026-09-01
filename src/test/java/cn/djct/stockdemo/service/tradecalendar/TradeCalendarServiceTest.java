package cn.djct.stockdemo.service.tradecalendar;

import cn.djct.stockdemo.face.TradeCalendarDataInitializer;
import cn.djct.stockdemo.mapper.NationalHolidayMapper;
import cn.djct.stockdemo.mapper.TradeCalendarMapper;
import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import cn.djct.stockdemo.service.tradecalendar.impl.TradeCalendarServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeCalendarServiceTest {

    @Mock
    private TradeCalendarMapper tradeCalendarMapper;

    @Mock
    private NationalHolidayMapper nationalHolidayMapper;

    @Mock
    private TradeCalendarDataInitializer tradeCalendarDataInitializer;

    @InjectMocks
    private TradeCalendarServiceImpl tradeCalendarService;

    @Test
    void shouldInitializeInBatchesOfFiveHundred() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 27);
        List<TradeCalendar> calendars = new ArrayList<>();
        for (int index = 0; index < 1001; index++) {
            calendars.add(TradeCalendar.builder().tradeDate(startDate.plusDays(index)).build());
        }

        when(tradeCalendarMapper.countByMarketCodeAndDateRange("CN_A", startDate, endDate)).thenReturn(0);
        when(nationalHolidayMapper.selectByDateRange(startDate, endDate)).thenReturn(List.of());
        when(tradeCalendarDataInitializer.generate("CN_A", startDate, endDate, Map.of()))
                .thenReturn(calendars);
        when(tradeCalendarMapper.insertBatch(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());

        int insertedCount = tradeCalendarService.initialize(startDate, endDate);

        assertEquals(1001, insertedCount);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TradeCalendar>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(tradeCalendarMapper, times(3)).insertBatch(batchCaptor.capture());
        assertEquals(List.of(500, 500, 1), batchCaptor.getAllValues().stream().map(List::size).toList());
    }

    @Test
    void shouldRejectRepeatedInitialization() {
        LocalDate startDate = LocalDate.of(2017, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        when(tradeCalendarMapper.countByMarketCodeAndDateRange("CN_A", startDate, endDate)).thenReturn(1);

        assertThrows(
                IllegalStateException.class,
                () -> tradeCalendarService.initialize(startDate, endDate)
        );

        verifyNoInteractions(nationalHolidayMapper, tradeCalendarDataInitializer);
        verify(tradeCalendarMapper, never()).insertBatch(anyList());
    }

    @Test
    void shouldReturnTradingDayResult() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(tradeCalendarMapper.selectByMarketCodeAndTradeDate("CN_A", date))
                .thenReturn(TradeCalendar.builder().isTradingDay(true).build());

        assertTrue(tradeCalendarService.isTradingDay(date));
    }

    @Test
    void shouldReturnSecondPreviousTradingDayAcrossHoliday() {
        LocalDate date = LocalDate.of(2023, 6, 26);
        LocalDate expected = LocalDate.of(2023, 6, 20);
        when(tradeCalendarMapper.selectPreviousTradingDate("CN_A", date, 1)).thenReturn(expected);

        assertEquals(expected, tradeCalendarService.getPreviousTradingDay(date, 2));
        verify(tradeCalendarMapper).selectPreviousTradingDate("CN_A", date, 1);
    }

    @Test
    void shouldReturnSecondNextTradingDay() {
        LocalDate date = LocalDate.of(2023, 6, 26);
        LocalDate expected = LocalDate.of(2023, 6, 28);
        when(tradeCalendarMapper.selectNextTradingDate("CN_A", date, 1)).thenReturn(expected);

        assertEquals(expected, tradeCalendarService.getNextTradingDay(date, 2));
        verify(tradeCalendarMapper).selectNextTradingDate("CN_A", date, 1);
    }
}
