package cn.djct.stockdemo.face;

import cn.djct.stockdemo.constant.TradeDayType;
import cn.djct.stockdemo.pojo.entity.NationalHoliday;
import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCalendarDataInitializerTest {

    private final TradeCalendarDataInitializer initializer = new TradeCalendarDataInitializer();

    @Test
    void shouldGenerateTradingHolidayAndWeekendDates() {
        LocalDate startDate = LocalDate.of(2024, 1, 5);
        LocalDate holidayDate = LocalDate.of(2024, 1, 6);
        LocalDate endDate = LocalDate.of(2024, 1, 7);
        NationalHoliday holiday = NationalHoliday.builder()
                .holidayDate(holidayDate)
                .holidayName("测试节假日")
                .sourceTitle("测试通知")
                .sourceUrl("https://example.com")
                .build();

        List<TradeCalendar> calendars = initializer.generate(
                "CN_A",
                startDate,
                endDate,
                Map.of(holidayDate, holiday)
        );

        assertEquals(3, calendars.size());
        assertTrue(calendars.get(0).getIsTradingDay());
        assertEquals(TradeDayType.TRADING_DAY, calendars.get(0).getDayType());
        assertFalse(calendars.get(1).getIsTradingDay());
        assertEquals(TradeDayType.NATIONAL_HOLIDAY, calendars.get(1).getDayType());
        assertEquals("测试节假日", calendars.get(1).getHolidayName());
        assertFalse(calendars.get(2).getIsTradingDay());
        assertEquals(TradeDayType.WEEKEND, calendars.get(2).getDayType());
    }

    @Test
    void shouldGenerateDatesAcrossYearBoundary() {
        List<TradeCalendar> calendars = initializer.generate(
                "CN_A",
                LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 1),
                Map.of()
        );

        assertEquals(2, calendars.size());
        assertEquals(LocalDate.of(2023, 12, 31), calendars.get(0).getTradeDate());
        assertEquals(LocalDate.of(2024, 1, 1), calendars.get(1).getTradeDate());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> initializer.generate(
                        "CN_A",
                        LocalDate.of(2024, 1, 2),
                        LocalDate.of(2024, 1, 1),
                        Map.of()
                )
        );
    }
}
