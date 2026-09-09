package cn.djct.stockdemo.service.indexstyle.impl;

import cn.djct.stockdemo.common.IndexEtfChangeCalculator;
import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.mapper.IndexEtfDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.IndexEtfComparisonDto;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexEtfServiceImplTest {

    @Mock
    private IndexEtfDailyQuoteMapper indexEtfDailyQuoteMapper;
    @Mock
    private TradeCalendarService tradeCalendarService;
    @Mock
    private IndexEtfChangeCalculator indexEtfChangeCalculator;

    @Test
    void shouldUseLatestCompleteDateAndPreviousFifthTradingDay() {
        LocalDate statisticsDate = LocalDate.of(2026, 9, 4);
        LocalDate baseTradeDate = LocalDate.of(2026, 8, 28);
        List<String> etfCodes = Arrays.stream(IndexEtf.values())
                .map(IndexEtf::getEtfCode)
                .toList();
        List<IndexEtfDailyQuote> quotes = List.of(
                quote("510050", baseTradeDate),
                quote("510050", statisticsDate)
        );
        IndexEtfServiceImpl indexEtfService = new IndexEtfServiceImpl(
                indexEtfDailyQuoteMapper,
                tradeCalendarService,
                indexEtfChangeCalculator
        );
        when(indexEtfDailyQuoteMapper.selectLatestTradeDate()).thenReturn(statisticsDate);
        when(tradeCalendarService.getPreviousTradingDay(statisticsDate, 5))
                .thenReturn(baseTradeDate);
        when(indexEtfDailyQuoteMapper.selectByTradeDatesAndCodes(
                List.of(baseTradeDate, statisticsDate),
                etfCodes
        )).thenReturn(quotes);
        when(indexEtfChangeCalculator.calculate(baseTradeDate, statisticsDate, quotes))
                .thenReturn(List.of());

        IndexEtfComparisonDto result = indexEtfService.getLatest();

        assertEquals(statisticsDate, result.getStatisticsDate());
        assertEquals(baseTradeDate, result.getBaseTradeDate());
        verify(indexEtfChangeCalculator).calculate(baseTradeDate, statisticsDate, quotes);
    }

    @Test
    void shouldRejectWhenNoStoredDateExists() {
        IndexEtfServiceImpl indexEtfService = new IndexEtfServiceImpl(
                indexEtfDailyQuoteMapper,
                tradeCalendarService,
                indexEtfChangeCalculator
        );
        when(indexEtfDailyQuoteMapper.selectLatestTradeDate()).thenReturn(null);

        assertThrows(IllegalStateException.class, indexEtfService::getLatest);
    }

    private IndexEtfDailyQuote quote(String etfCode, LocalDate tradeDate) {
        return IndexEtfDailyQuote.builder()
                .etfCode(etfCode)
                .tradeDate(tradeDate)
                .build();
    }
}
