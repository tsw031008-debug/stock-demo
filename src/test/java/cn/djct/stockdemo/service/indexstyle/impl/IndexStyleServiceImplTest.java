package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexStyleCalculator;
import cn.djct.stockdemo.constant.IndexStyleIndex;
import cn.djct.stockdemo.mapper.IndexDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexStyleServiceImplTest {

    @Mock
    private IndexDailyQuoteMapper indexDailyQuoteMapper;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private IndexStyleCalculator indexStyleCalculator;
    @InjectMocks
    private IndexStyleServiceImpl indexStyleService;

    @Test
    void shouldQueryLatestFiveTradingDays() {
        LocalDate statisticsDate = LocalDate.of(2026, 9, 3);
        List<LocalDate> tradeDates = List.of(
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                statisticsDate
        );
        List<String> codes = Arrays.stream(IndexStyleIndex.values())
                .map(IndexStyleIndex::getIndexCode)
                .toList();
        List<IndexDailyQuote> quotes = new ArrayList<>();
        for (LocalDate tradeDate : tradeDates) {
            for (IndexStyleIndex index : IndexStyleIndex.values()) {
                quotes.add(IndexDailyQuote.builder()
                        .indexCode(index.getIndexCode())
                        .tradeDate(tradeDate)
                        .closePrice(new BigDecimal("100"))
                        .previousClosePrice(new BigDecimal("99"))
                        .build());
            }
        }
        when(indexDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 4))
                .thenReturn(tradeDates.get(0));
        when(tradeCalendarService.getTradingDays(tradeDates.get(0), statisticsDate))
                .thenReturn(tradeDates);
        when(indexDailyQuoteMapper.selectByTradeDatesAndCodes(tradeDates, codes))
                .thenReturn(quotes);
        when(indexStyleCalculator.calculate(tradeDates, quotes)).thenReturn(List.of());

        IndexStyleComparisonDto result = indexStyleService.getLatest();

        assertEquals(statisticsDate, result.getStatisticsDate());
        assertEquals(tradeDates, result.getTradeDates());
    }
}
