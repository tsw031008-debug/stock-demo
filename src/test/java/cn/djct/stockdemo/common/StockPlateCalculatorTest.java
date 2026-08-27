package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.StockPlateLimitUpDto;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.entity.StockPlateDailyQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockPlateCalculatorTest {

    private StockPlateCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new StockPlateCalculator(new StockAlertCalculator());
    }

    @Test
    void shouldCalculateFirstTradingDayDailyQuoteFromOneThousand() {
        List<StockPlateDailyQuote> result = calculator.calculateDailyQuotes(
                LocalDate.of(2026, 1, 5),
                members(),
                quotes(),
                Map.of(),
                true,
                "PARTIAL"
        );

        StockPlateDailyQuote plate = result.get(0);
        assertEquals(new BigDecimal("1000.0000"), plate.getOpenPrice());
        assertEquals(new BigDecimal("1050.0000"), plate.getClosePrice());
        assertEquals(new BigDecimal("5.0000"), plate.getChangePercent());
        assertEquals(new BigDecimal("300.00"), plate.getTurnoverAmountYuan());
        assertEquals(2, plate.getStockCount());
        assertEquals("PARTIAL", plate.getDataStatus());
    }

    @Test
    void shouldUsePreviousCloseForNextTradingDay() {
        List<StockPlateDailyQuote> result = calculator.calculateDailyQuotes(
                LocalDate.of(2026, 1, 6),
                members(),
                quotes(),
                Map.of(1L, new BigDecimal("1050.0000")),
                false,
                "COMPLETE"
        );

        StockPlateDailyQuote plate = result.get(0);
        assertEquals(new BigDecimal("1050.0000"), plate.getOpenPrice());
        assertEquals(new BigDecimal("1102.5000"), plate.getClosePrice());
        assertEquals("COMPLETE", plate.getDataStatus());
    }

    @Test
    void shouldCalculateLimitUpCountRatioAndSort() {
        List<StockPlateLimitUpDto> result = calculator.calculateLimitUpStatistics(
                members(),
                quotes()
        );

        assertEquals(List.of("科技-概", "金融-概"), result.stream()
                .map(StockPlateLimitUpDto::getPlateName)
                .toList());
        StockPlateLimitUpDto technology = result.get(0);
        assertEquals(2, technology.getTotalStockCount());
        assertEquals(new BigDecimal("5.00"), technology.getPlateChangePercent());
        assertEquals(1, technology.getLimitUpStockCount());
        assertEquals(new BigDecimal("50.00"), technology.getLimitUpRatio());
    }

    /**
     * 创建两个板块的成分关系，其中000002同时属于两个板块。
     */
    private List<StockPlateMemberDto> members() {
        return List.of(
                member(1L, "科技-概", "000001"),
                member(1L, "科技-概", "000002"),
                member(1L, "科技-概", "000002"),
                member(2L, "金融-概", "000002")
        );
    }

    /**
     * 创建一只涨停候选和一只普通股票行情。
     */
    private List<StockDailyQuote> quotes() {
        return List.of(
                quote("000001", "示例一", "10.00", "100.00", 0L),
                quote("000002", "示例二", "0.00", "200.00", 5L)
        );
    }

    /**
     * 创建板块成分关系。
     */
    private StockPlateMemberDto member(Long plateId, String plateName, String stockCode) {
        return StockPlateMemberDto.builder()
                .plateId(plateId)
                .plateName(plateName)
                .stockCode(stockCode)
                .build();
    }

    /**
     * 创建计算所需股票行情。
     */
    private StockDailyQuote quote(
            String stockCode,
            String stockName,
            String changePercent,
            String turnoverAmount,
            Long ask1Volume
    ) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .closePrice(BigDecimal.TEN)
                .changePercent(new BigDecimal(changePercent))
                .turnoverAmountYuan(new BigDecimal(turnoverAmount))
                .ask1VolumeHand(ask1Volume)
                .build();
    }
}
