package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockAlertCalculatorTest {

    private static final LocalDate T_1 = LocalDate.of(2026, 8, 21);
    private static final LocalDate T_2 = LocalDate.of(2026, 8, 20);

    private final StockAlertCalculator calculator = new StockAlertCalculator();

    @Test
    void shouldReturnOnlyStocksWhoseThreeDaySlopeExceedsTwoDaySlope() {
        List<StockDailyQuote> quotes = List.of(
                createQuote("000001", "平安银行", "12", "8.126", 1L, "100000000"),
                createQuote("000002", "万科A", "12", "9.00", 1L, "100000000"),
                createQuote("000003", "测试股份", "10", "7.00", 1L, "100000000")
        );
        List<StockClosePriceDto> closePrices = List.of(
                closePrice("000001", T_1, "11"),
                closePrice("000001", T_2, "10"),
                closePrice("000002", T_1, "10"),
                closePrice("000002", T_2, "10"),
                closePrice("000003", T_1, "10"),
                closePrice("000003", T_2, "10")
        );

        List<StockSpeedAlertDto> result = calculator.calculateSpeedAlerts(
                quotes,
                closePrices,
                T_1,
                T_2
        );

        assertEquals(1, result.size());
        assertEquals("000001", result.get(0).getStockCode());
        assertEquals(new BigDecimal("12.00"), result.get(0).getCurrentPrice());
        assertEquals(new BigDecimal("8.13"), result.get(0).getCurrentChangePercent());
    }

    @Test
    void shouldSortSpeedAlertsByCurrentChangeThenSlopeDifferenceThenCode() {
        List<StockDailyQuote> quotes = List.of(
                createQuote("000003", "股票三", "12", "9", 1L, "100000000"),
                createQuote("000002", "股票二", "15", "10", 1L, "100000000"),
                createQuote("000001", "股票一", "12", "10", 1L, "100000000")
        );
        List<StockClosePriceDto> closePrices = List.of(
                closePrice("000003", T_1, "11"), closePrice("000003", T_2, "10"),
                closePrice("000002", T_1, "14"), closePrice("000002", T_2, "10"),
                closePrice("000001", T_1, "11"), closePrice("000001", T_2, "10")
        );

        List<StockSpeedAlertDto> result = calculator.calculateSpeedAlerts(
                quotes,
                closePrices,
                T_1,
                T_2
        );

        assertEquals(List.of("000002", "000001", "000003"), result.stream()
                .map(StockSpeedAlertDto::getStockCode)
                .toList());
    }

    @Test
    void shouldSkipSpeedAlertWhenHistoricalPriceIsMissingOrZero() {
        List<StockDailyQuote> quotes = List.of(
                createQuote("000001", "平安银行", "12", "8", 1L, "100000000"),
                createQuote("000002", "万科A", "12", "8", 1L, "100000000")
        );
        List<StockClosePriceDto> closePrices = List.of(
                closePrice("000001", T_1, "11"),
                closePrice("000002", T_1, "11"),
                closePrice("000002", T_2, "0")
        );

        assertTrue(calculator.calculateSpeedAlerts(quotes, closePrices, T_1, T_2).isEmpty());
    }

    @Test
    void shouldFilterAndSortOpenBoardAlerts() {
        List<StockDailyQuote> quotes = List.of(
                createQuote("000001", "平安银行", "10.126", "10.005", 0L, "250000000"),
                createQuote("000002", "万科A", "12", "10.005", 0L, "300000000"),
                createQuote("000003", "招商银行", "15", "9", 1L, "400000000"),
                createQuote("000004", "ST测试", "8", "10", 0L, "100000000"),
                createQuote("000005", "*ST测试", "8", "10", 0L, "100000000"),
                createQuote("000006", "零成交", "8", "10", 0L, "0")
        );

        List<StockOpenBoardAlertDto> result = calculator.calculateOpenBoardAlerts(quotes);

        assertEquals(List.of("000002", "000001"), result.stream()
                .map(StockOpenBoardAlertDto::getStockCode)
                .toList());
        assertEquals(new BigDecimal("10.13"), result.get(1).getCurrentPrice());
        assertEquals(new BigDecimal("2.50"), result.get(1).getTurnoverYi());
    }

    @Test
    void shouldNotTreatMissingAskVolumeAsZero() {
        StockDailyQuote quote = createQuote(
                "000001",
                "平安银行",
                "10",
                "10",
                null,
                "100000000"
        );

        assertTrue(calculator.calculateOpenBoardAlerts(List.of(quote)).isEmpty());
    }

    private StockDailyQuote createQuote(
            String stockCode,
            String stockName,
            String currentPrice,
            String changePercent,
            Long ask1VolumeHand,
            String turnoverAmountYuan
    ) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .closePrice(new BigDecimal(currentPrice))
                .changePercent(new BigDecimal(changePercent))
                .ask1VolumeHand(ask1VolumeHand)
                .turnoverAmountYuan(new BigDecimal(turnoverAmountYuan))
                .build();
    }

    private StockClosePriceDto closePrice(String stockCode, LocalDate tradeDate, String closePrice) {
        return StockClosePriceDto.builder()
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .build();
    }
}
