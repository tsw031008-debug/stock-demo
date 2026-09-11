package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.vo.TechnologyStockRankingRespVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TechnologyStockRankingCalculatorTest {

    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 9, 7);
    private static final LocalDate FIVE_DAY_BASE_DATE = LocalDate.of(2026, 8, 31);
    private static final LocalDate TEN_DAY_BASE_DATE = LocalDate.of(2026, 8, 24);
    private static final LocalDate FIFTEEN_DAY_BASE_DATE = LocalDate.of(2026, 8, 17);
    private static final LocalDate TWENTY_DAY_BASE_DATE = LocalDate.of(2026, 8, 10);
    private static final LocalDate SIXTY_DAY_BASE_DATE = LocalDate.of(2026, 6, 12);

    private final TechnologyStockRankingCalculator calculator =
            new TechnologyStockRankingCalculator();

    @Test
    void shouldCalculateIndependentTopFiveRankingsAndAllowOverlap() {
        List<StockDailyQuote> quotes = new ArrayList<>();
        quotes.addAll(quotes("000001", "科技一", "120", "-9", "100000001",
                "100", "100", "110", "100", "100"));
        quotes.addAll(quotes("000002", "科技二", "118", "8", "100000001",
                "100", "100", "110", "100", "100"));
        quotes.addAll(quotes("000003", "科技三", "116", "7", "100000001",
                "100", "100", "110", "100", "100"));
        quotes.addAll(quotes("000004", "科技四", "114", "6", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000005", "科技五", "112", "5", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000006", "科技六", "110", "4", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000007", "*ST科技", "200", "10", "500000000",
                "100", "100", "180", "100", "100"));

        TechnologyStockRankingRespVo result = calculate(quotes);

        assertEquals(List.of("000001", "000002", "000003", "000004", "000005"),
                result.getHotStocks().stream().map(item -> item.getStockCode()).toList());
        assertEquals(List.of("000001", "000002", "000003", "000004", "000005"),
                result.getPotentialStocks().stream().map(item -> item.getStockCode()).toList());
        assertEquals(List.of(1, 2, 3, 4, 5),
                result.getPotentialStocks().stream().map(item -> item.getRank()).toList());
        assertEquals(STATISTICS_DATE, result.getStatisticsDate());
    }

    @Test
    void shouldUseStrictPotentialBoundaries() {
        List<StockDailyQuote> quotes = new ArrayList<>();
        quotes.addAll(quotes("000001", "有效科技", "110", "1", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000002", "十五日等于边界", "115", "2", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000003", "十日等于边界", "101", "3", "100000001",
                "100", "100", "100", "90", "100"));
        quotes.addAll(quotes("000004", "五日等于边界", "110", "4", "100000001",
                "110", "100", "100", "100", "100"));
        quotes.addAll(quotes("000005", "二十日等于边界", "105", "5", "100000001",
                "100", "100", "100", "100", "100"));
        quotes.addAll(quotes("000006", "六十日等于边界", "110", "6", "100000001",
                "100", "100", "100", "100", "110"));
        quotes.addAll(quotes("000007", "成交额等于边界", "110", "7", "100000000",
                "100", "100", "100", "100", "100"));

        TechnologyStockRankingRespVo result = calculate(quotes);

        assertEquals(List.of("000001"), result.getPotentialStocks().stream()
                .map(item -> item.getStockCode())
                .toList());
    }

    @Test
    void shouldKeepHotCandidateWhenSixtyDayQuoteIsMissing() {
        List<StockDailyQuote> quotes = new ArrayList<>(quotes(
                "000001", "新股科技", "110", "5", "200000000",
                "100", "100", "100", "100", "100"
        ));
        quotes.removeIf(quote -> SIXTY_DAY_BASE_DATE.equals(quote.getTradeDate()));

        TechnologyStockRankingRespVo result = calculate(quotes);

        assertEquals(List.of("000001"), result.getHotStocks().stream()
                .map(item -> item.getStockCode())
                .toList());
        assertEquals(List.of(), result.getPotentialStocks());
    }

    @Test
    void shouldRejectDuplicateStockQuote() {
        List<StockDailyQuote> quotes = new ArrayList<>(quotes(
                "000001", "科技一", "110", "5", "200000000",
                "100", "100", "100", "100", "100"
        ));
        quotes.add(quote("000001", "科技一", STATISTICS_DATE, "110", "5", "200000000"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> calculate(quotes)
        );

        assertEquals("科技股日行情重复，stockCode=000001，tradeDate=2026-09-07",
                exception.getMessage());
    }

    @Test
    void shouldOnlyExcludeNamesContainingUppercaseST() {
        List<StockDailyQuote> data = new ArrayList<>();
        for (String name : List.of("ST科技", "*ST科技", "科技ST", "st科技")) {
            data.addAll(quotes(String.format("%06d", data.size() + 1), name,
                    "110", "-5", "200000000", "100", "100", "100", "100", "100"));
        }
        TechnologyStockRankingRespVo result = calculate(data);
        assertEquals(List.of("st科技"), result.getHotStocks().stream()
                .map(item -> item.getStockName()).toList());
        assertEquals(List.of("st科技"), result.getPotentialStocks().stream()
                .map(item -> item.getStockName()).toList());
    }

    @Test
    void shouldNotApplyPotentialRequirementsToHotRanking() {
        List<StockDailyQuote> data = List.of(
                quote("000001", "科技一", STATISTICS_DATE, "90", null, null),
                quote("000001", "科技一", TWENTY_DAY_BASE_DATE, "100", null, null)
        );
        TechnologyStockRankingRespVo result = calculate(data);
        // 热门榜允许负20日涨幅，不要求当日涨幅、成交额或其他周期价格。
        assertEquals("000001", result.getHotStocks().get(0).getStockCode());
        assertEquals(List.of(), result.getPotentialStocks());
    }

    @Test
    void shouldResolveEqualScoresByStockCodeAtFifthPlace() {
        List<StockDailyQuote> data = new ArrayList<>();
        for (int index = 6; index >= 1; index--) {
            data.addAll(quotes(String.format("%06d", index), "科技", "110",
                    index % 2 == 0 ? "-5" : "5", "200000000",
                    "100", "100", "100", "100", "100"));
        }
        TechnologyStockRankingRespVo result = calculate(data);
        List<String> expected = List.of("000001", "000002", "000003", "000004", "000005");
        assertEquals(expected, result.getHotStocks().stream()
                .map(item -> item.getStockCode()).toList());
        assertEquals(expected, result.getPotentialStocks().stream()
                .map(item -> item.getStockCode()).toList());
    }

    private TechnologyStockRankingRespVo calculate(List<StockDailyQuote> quotes) {
        return calculator.calculate(
                STATISTICS_DATE,
                FIVE_DAY_BASE_DATE,
                TEN_DAY_BASE_DATE,
                FIFTEEN_DAY_BASE_DATE,
                TWENTY_DAY_BASE_DATE,
                SIXTY_DAY_BASE_DATE,
                quotes
        );
    }

    private List<StockDailyQuote> quotes(
            String stockCode,
            String stockName,
            String currentClose,
            String changePercent,
            String turnoverAmount,
            String fiveDayClose,
            String tenDayClose,
            String fifteenDayClose,
            String twentyDayClose,
            String sixtyDayClose
    ) {
        return List.of(
                quote(stockCode, stockName, STATISTICS_DATE,
                        currentClose, changePercent, turnoverAmount),
                quote(stockCode, stockName, FIVE_DAY_BASE_DATE, fiveDayClose, null, null),
                quote(stockCode, stockName, TEN_DAY_BASE_DATE, tenDayClose, null, null),
                quote(stockCode, stockName, FIFTEEN_DAY_BASE_DATE, fifteenDayClose, null, null),
                quote(stockCode, stockName, TWENTY_DAY_BASE_DATE, twentyDayClose, null, null),
                quote(stockCode, stockName, SIXTY_DAY_BASE_DATE, sixtyDayClose, null, null)
        );
    }

    private StockDailyQuote quote(
            String stockCode,
            String stockName,
            LocalDate tradeDate,
            String closePrice,
            String changePercent,
            String turnoverAmount
    ) {
        return StockDailyQuote.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .tradeDate(tradeDate)
                .closePrice(decimal(closePrice))
                .changePercent(decimal(changePercent))
                .turnoverAmountYuan(decimal(turnoverAmount))
                .build();
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
