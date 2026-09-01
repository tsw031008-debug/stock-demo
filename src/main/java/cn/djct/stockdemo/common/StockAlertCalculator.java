package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.StockClosePriceDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 股票预警计算组件。
 */
@Component
public class StockAlertCalculator {

    private static final int CALCULATION_SCALE = 8;
    private static final int RESPONSE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");

    /**
     * 根据当前价、T-1和T-2收盘价筛选涨速预警股票。
     *
     * @param currentQuotes 当前实时行情
     * @param closePrices   历史收盘价
     * @param previousDate  T-1交易日
     * @param twoDaysAgo    T-2交易日
     * @return 按当前涨幅倒序排列的涨速预警
     */
    public List<StockSpeedAlertDto> calculateSpeedAlerts(
            List<StockDailyQuote> currentQuotes,
            List<StockClosePriceDto> closePrices,
            LocalDate previousDate,
            LocalDate twoDaysAgo
    ) {
        // 校验涨速计算所需数据和交易日不能为空
        Objects.requireNonNull(currentQuotes, "当前实时行情不能为空");
        Objects.requireNonNull(closePrices, "历史收盘价不能为空");
        Objects.requireNonNull(previousDate, "T-1交易日不能为空");
        Objects.requireNonNull(twoDaysAgo, "T-2交易日不能为空");

        // 按股票代码和交易日组织历史价格，避免逐只股票重复遍历历史数据
        Map<String, Map<LocalDate, BigDecimal>> pricesByStock = groupClosePrices(closePrices);
        // 计算单只股票斜率，过滤未命中股票并按业务规则排序
        return currentQuotes.stream()
                // 把每只符合涨速条件的股票转换成结果对象，不符合转为null
                .map(quote -> calculateSpeedAlert(quote, pricesByStock, previousDate, twoDaysAgo))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                                StockSpeedAlertDto::getCurrentChangePercent,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                StockSpeedAlertDto::getSlopeDifference,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(StockSpeedAlertDto::getStockCode))
                .toList();
    }

    /**
     * 按文档条件筛选开板提醒股票。
     *
     * @param currentQuotes 当前实时行情
     * @return 按当前涨幅倒序排列的开板提醒
     */
    public List<StockOpenBoardAlertDto> calculateOpenBoardAlerts(List<StockDailyQuote> currentQuotes) {
        // 校验实时行情不能为空
        Objects.requireNonNull(currentQuotes, "当前实时行情不能为空");
        // 按开板条件筛选，转换成交额单位并按业务规则排序
        return currentQuotes.stream()
                .filter(this::isLimitUpCandidate)
                .map(quote -> StockOpenBoardAlertDto.builder()
                        .stockCode(quote.getStockCode())
                        .stockName(quote.getStockName())
                        .currentPrice(toResponseScale(quote.getClosePrice()))
                        .currentChangePercent(toResponseScale(quote.getChangePercent()))
                        .turnoverYi(quote.getTurnoverAmountYuan()
                                .divide(ONE_HUNDRED_MILLION, RESPONSE_SCALE, RoundingMode.HALF_UP))
                        .build())
                .sorted(Comparator.comparing(
                                StockOpenBoardAlertDto::getCurrentChangePercent,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                StockOpenBoardAlertDto::getTurnoverYi,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(StockOpenBoardAlertDto::getStockCode))
                .toList();
    }

    /**
     * 将历史收盘价按股票和交易日组织，便于计算时读取。
     */
    private Map<String, Map<LocalDate, BigDecimal>> groupClosePrices(
            List<StockClosePriceDto> closePrices
    ) {
        // 忽略字段不完整的历史记录，空值不能作为0参与计算
        Map<String, Map<LocalDate, BigDecimal>> pricesByStock = new HashMap<>();
        for (StockClosePriceDto closePrice : closePrices) {
            if (closePrice == null || closePrice.getStockCode() == null
                    || closePrice.getTradeDate() == null || closePrice.getClosePrice() == null) {
                continue;
            }
            // 同一股票的历史收盘价按交易日保存
            pricesByStock.computeIfAbsent(closePrice.getStockCode(), key -> new HashMap<>())
                    .put(closePrice.getTradeDate(), closePrice.getClosePrice());
        }
        return pricesByStock;
    }

    /**
     * 计算单只股票的两日和三日斜率，三日斜率严格大于两日斜率时返回结果。
     */
    private StockSpeedAlertDto calculateSpeedAlert(
            // 当前行情
            StockDailyQuote quote,
            // 历史价格
            Map<String, Map<LocalDate, BigDecimal>> pricesByStock,
            // T-1交易日
            LocalDate previousDate,
            // T-2交易日
            LocalDate twoDaysAgo
    ) {
        // 当前行情字段不完整或当前价无效时不参与计算
        if (!hasRequiredSpeedFields(quote)) {
            return null;
        }
        // 缺少该股票历史价格时不参与计算
        Map<LocalDate, BigDecimal> historicalPrices = pricesByStock.get(quote.getStockCode());
        if (historicalPrices == null) {
            return null;
        }
        // 获取T-1和T-2收盘价
        BigDecimal previousClosePrice = historicalPrices.get(previousDate);
        BigDecimal twoDaysAgoClosePrice = historicalPrices.get(twoDaysAgo);
        // T-1或T-2价格缺失、为0时不能计算涨幅
        if (!isPositive(previousClosePrice) || !isPositive(twoDaysAgoClosePrice)) {
            return null;
        }

        // 两日涨幅使用T-1收盘价并除以2，三日涨幅使用T-2收盘价并除以3
        BigDecimal twoDaySlope = calculateGain(quote.getClosePrice(), previousClosePrice)
                .divide(BigDecimal.valueOf(2), CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal threeDaySlope = calculateGain(quote.getClosePrice(), twoDaysAgoClosePrice)
                .divide(BigDecimal.valueOf(3), CALCULATION_SCALE, RoundingMode.HALF_UP);
        // 只有三日斜率严格大于两日斜率才进入预警结果
        if (threeDaySlope.compareTo(twoDaySlope) <= 0) {
            return null;
        }

        // 保留斜率差供结果二级排序，对外响应不返回该字段
        return StockSpeedAlertDto.builder()
                .stockCode(quote.getStockCode())
                .stockName(quote.getStockName())
                .currentPrice(toResponseScale(quote.getClosePrice()))
                .currentChangePercent(toResponseScale(quote.getChangePercent()))
                .slopeDifference(threeDaySlope.subtract(twoDaySlope))
                .build();
    }

    /**
     * 计算当前价相对历史收盘价的涨幅百分比。
     */
    private BigDecimal calculateGain(BigDecimal currentPrice, BigDecimal historicalPrice) {
        // 涨幅=(当前价/历史收盘价-1)*100%
        return currentPrice.divide(historicalPrice, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(ONE_HUNDRED);
    }

    /**
     * 判断涨速计算所需的当前行情字段是否完整。
     */
    private boolean hasRequiredSpeedFields(StockDailyQuote quote) {
        // 股票标识、名称、当前涨幅和正数当前价都是涨速计算的必需字段
        return quote != null
                && quote.getStockCode() != null
                && quote.getStockName() != null
                && quote.getChangePercent() != null
                && isPositive(quote.getClosePrice());
    }

    /**
     * 判断行情是否符合卖一量为0、有效成交且非ST的文档条件。
     */
    public boolean isLimitUpCandidate(StockDailyQuote quote) {
        // 必需字段为空时不能把空值当成0命中筛选条件
        if (quote == null || quote.getStockCode() == null || quote.getStockName() == null
                || quote.getChangePercent() == null || quote.getAsk1VolumeHand() == null) {
            return false;
        }
        // 仅按名称前缀排除ST和*ST，不扩大过滤范围
        String stockName = quote.getStockName().trim();
        return !stockName.startsWith("ST")
                && !stockName.startsWith("*ST")
                && quote.getAsk1VolumeHand() == 0L
                && isPositive(quote.getTurnoverAmountYuan())
                && isPositive(quote.getClosePrice());
    }

    /**
     * 判断金额或价格是否大于0。
     */
    private boolean isPositive(BigDecimal value) {
        // 金额和价格必须存在且严格大于0
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 将接口展示数值统一保留2位小数。
     */
    private BigDecimal toResponseScale(BigDecimal value) {
        // 接口展示字段统一四舍五入保留2位小数
        return value.setScale(RESPONSE_SCALE, RoundingMode.HALF_UP);
    }
}
