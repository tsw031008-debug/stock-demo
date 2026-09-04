package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.constant.StockCustomCategory;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverComparisonDto;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverItemDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.dto.StockTurnoverByDateDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.service.plate.StockCategoryTurnoverComparisonService;
import cn.djct.stockdemo.service.plate.StockCustomPlateService;
import cn.djct.stockdemo.service.stockdailyquote.StockDailyQuoteSourceService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 四大类当前与上一交易日成交额对比服务实现。
 */
@Service
public class StockCategoryTurnoverComparisonServiceImpl
        implements StockCategoryTurnoverComparisonService {

    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockCustomPlateService stockCustomPlateService;
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final StockDailyQuoteSourceService stockDailyQuoteSourceService;
    private final TradeCalendarService tradeCalendarService;
    private final Clock clock;

    /**
     * 创建使用系统时钟的四大类成交额对比服务。
     */
    @Autowired
    public StockCategoryTurnoverComparisonServiceImpl(
            StockCustomPlateService stockCustomPlateService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            TradeCalendarService tradeCalendarService
    ) {
        this(
                stockCustomPlateService,
                stockDailyQuoteMapper,
                stockDailyQuoteSourceService,
                tradeCalendarService,
                Clock.system(SHANGHAI_ZONE)
        );
    }

    /**
     * 创建使用指定时钟的服务，供交易日期和开盘边界测试使用。
     */
    StockCategoryTurnoverComparisonServiceImpl(
            StockCustomPlateService stockCustomPlateService,
            StockDailyQuoteMapper stockDailyQuoteMapper,
            StockDailyQuoteSourceService stockDailyQuoteSourceService,
            TradeCalendarService tradeCalendarService,
            Clock clock
    ) {
        this.stockCustomPlateService = stockCustomPlateService;
        this.stockDailyQuoteMapper = stockDailyQuoteMapper;
        this.stockDailyQuoteSourceService = stockDailyQuoteSourceService;
        this.tradeCalendarService = tradeCalendarService;
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
    }

    /**
     * 计算当前交易日实时成交额与上一交易日已落库成交额。
     *
     * 先按四大类分别合并并去重子板块成分股，再将全部大类股票合并，统一查询今日和
     * 上一交易日成交额，避免重复请求。查询完成后仍使用每个大类自己的成分股集合分别
     * 汇总：同一大类内的重复股票只计算一次，同一股票属于不同大类时分别计入对应大类。
     *
     * @return 四大类两个交易日的成交额对比
     */
    @Override
    public StockCategoryTurnoverComparisonDto getCurrent() {
        LocalDate statisticsDate = validateQueryTime();
        LocalDate previousTradeDate = tradeCalendarService.getPreviousTradingDay(statisticsDate, 1);
        // 获取四大类各自对应的成分股，各自进行去重
        Map<StockCustomCategory, Set<String>> categoryStocks = groupCategoryStocks(
                // 获取所有自定义板块的成分股
                stockCustomPlateService.findActiveMemberRelations()
        );
        // 合并全部大类的成分股，只是为了统一查询原始成交额，用于之后计算各自大类的成交额
        List<String> stockCodes = categoryStocks.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .sorted()
                .toList();
        // 准备一个空列表，用于存放今日和昨日的成交额记录
        List<StockTurnoverByDateDto> turnoverRecords = new ArrayList<>();
        // 调用腾讯接口获取当前交易日实时行情
        turnoverRecords.addAll(toCurrentTurnovers(
                statisticsDate,
                stockCodes,
                stockDailyQuoteSourceService.fetchByCodes(statisticsDate, stockCodes)
        ));
        // 昨日使用已落库日行情
        turnoverRecords.addAll(stockDailyQuoteMapper.selectTurnoversByTradeDates(
                List.of(previousTradeDate),
                stockCodes
        ));
        // 建立“交易日 -> 股票代码 -> 成交额”索引，供每个大类分别取值汇总。
        Map<LocalDate, Map<String, BigDecimal>> turnovers = indexTurnovers(turnoverRecords);

        // 使用各大类自己的去重成分股，分别计算今日和昨日成交额。
        List<StockCategoryTurnoverItemDto> categories = Arrays.stream(StockCustomCategory.values())
                .map(category -> buildItem(
                        category,
                        categoryStocks.get(category),
                        statisticsDate,
                        previousTradeDate,
                        turnovers
                ))
                .toList();
        return StockCategoryTurnoverComparisonDto.builder()
                .statisticsDate(statisticsDate)
                .previousTradeDate(previousTradeDate)
                .categories(categories)
                .build();
    }

    /**
     * 当天必须是交易日且已经开盘，防止把上一交易日行情误当作今日成交额。
     */
    private LocalDate validateQueryTime() {
        LocalDate currentDate = LocalDate.now(clock);
        if (!tradeCalendarService.isTradingDay(currentDate)) {
            throw new IllegalStateException("当前为非交易日");
        }
        if (LocalTime.now(clock).isBefore(MARKET_OPEN_TIME)) {
            throw new IllegalStateException("当前交易日尚未开盘");
        }
        return currentDate;
    }

    /**
     * 将有效子板块成员按四大类分组，同一大类的重复股票在这里去重。
     */
    private Map<StockCustomCategory, Set<String>> groupCategoryStocks(
            List<StockCustomPlateMemberRelationDto> relations
    ) {
        Map<StockCustomCategory, Set<String>> categoryStocks = new HashMap<>();
        for (StockCustomPlateMemberRelationDto relation : relations) {
            StockCustomCategory category = StockCustomCategory.fromCode(relation.getCategoryCode());
            categoryStocks.computeIfAbsent(category, ignored -> new LinkedHashSet<>())
                    .add(relation.getStockCode());
        }
        for (StockCustomCategory category : StockCustomCategory.values()) {
            if (categoryStocks.getOrDefault(category, Set.of()).isEmpty()) {
                throw new IllegalStateException("板块大类没有有效成分股：" + category.getDisplayName());
            }
        }
        return categoryStocks;
    }

    /**
     * 校验今日实时行情，并转换成与昨日数据库查询结果相同的成交额记录。
     *
     * 本方法只负责数据校验和结构转换，不计算四大类成交额；转换后今日和昨日数据可以
     * 统一建立交易日、股票代码和成交额索引。
     */
    private List<StockTurnoverByDateDto> toCurrentTurnovers(
            LocalDate tradeDate,
            List<String> expectedStockCodes,
            List<StockDailyQuote> quotes
    ) {
        if (quotes == null) {
            throw new IllegalStateException("当前交易日实时行情为空");
        }
        Set<String> expectedCodes = new LinkedHashSet<>(expectedStockCodes);
        Set<String> actualCodes = new LinkedHashSet<>();
        List<StockTurnoverByDateDto> records = new ArrayList<>(quotes.size());
        for (StockDailyQuote quote : quotes) {
            if (quote == null || quote.getStockCode() == null || quote.getQuoteTime() == null
                    || quote.getTurnoverAmountYuan() == null) {
                throw new IllegalStateException("当前交易日实时成交额存在空值");
            }
            if (!tradeDate.equals(quote.getQuoteTime().toLocalDate())) {
                throw new IllegalStateException(
                        "实时行情日期不是当前交易日，stockCode=" + quote.getStockCode()
                                + "，quoteTime=" + quote.getQuoteTime()
                );
            }
            if (!expectedCodes.contains(quote.getStockCode())) {
                throw new IllegalStateException("实时行情包含未请求股票：" + quote.getStockCode());
            }
            if (!actualCodes.add(quote.getStockCode())) {
                throw new IllegalStateException("实时行情包含重复股票：" + quote.getStockCode());
            }
            records.add(StockTurnoverByDateDto.builder()
                    .stockCode(quote.getStockCode())
                    .tradeDate(tradeDate)
                    .turnoverAmountYuan(quote.getTurnoverAmountYuan())
                    .build());
        }
        if (!actualCodes.equals(expectedCodes)) {
            Set<String> missingCodes = new LinkedHashSet<>(expectedCodes);
            missingCodes.removeAll(actualCodes);
            throw new IllegalStateException("当前交易日实时行情不完整，missingStockCodes=" + missingCodes);
        }
        return records;
    }

    /**
     * 合并今日和昨日成交额，建立“交易日 -> 股票代码 -> 成交额”索引。
     * 空字段或同一股票同一交易日的重复记录不允许进入后续分类汇总。
     */
    private Map<LocalDate, Map<String, BigDecimal>> indexTurnovers(
            List<StockTurnoverByDateDto> records
    ) {
        Map<LocalDate, Map<String, BigDecimal>> result = new HashMap<>();
        for (StockTurnoverByDateDto record : records) {
            if (record.getTradeDate() == null || record.getStockCode() == null
                    || record.getTurnoverAmountYuan() == null) {
                throw new IllegalStateException("股票成交额原始数据存在空值");
            }
            BigDecimal previous = result
                    .computeIfAbsent(record.getTradeDate(), ignored -> new HashMap<>())
                    .put(record.getStockCode(), record.getTurnoverAmountYuan());
            if (previous != null) {
                throw new IllegalStateException(
                        "股票成交额存在重复记录，tradeDate=" + record.getTradeDate()
                                + "，stockCode=" + record.getStockCode()
                );
            }
        }
        return result;
    }

    /**
     * 汇总一个大类在两个交易日的成交额，并统一转换为亿元。
     * @param category        大类
     * @param stockCodes      成分股代码
     * @param statisticsDate  统计日
     * @param previousTradeDate  上一交易日
     * @param turnovers     成交额
     */
    private StockCategoryTurnoverItemDto buildItem(
            StockCustomCategory category,
            Set<String> stockCodes,
            LocalDate statisticsDate,
            LocalDate previousTradeDate,
            Map<LocalDate, Map<String, BigDecimal>> turnovers
    ) {
        // 计算今日和昨日的成交额
        BigDecimal current = sumRequired(category, stockCodes, statisticsDate, turnovers);
        BigDecimal previous = sumRequired(category, stockCodes, previousTradeDate, turnovers);
        return StockCategoryTurnoverItemDto.builder()
                .categoryCode(category.name())
                .categoryName(category.getDisplayName())
                .stockCount(stockCodes.size())
                // 数据库存人民币元，接口统一转换为亿元并保留两位小数。
                .currentTurnoverYi(toYi(current))
                .previousTurnoverYi(toYi(previous))
                .build();
    }

    /**
     * 汇总指定交易日的成分股成交额；任一股票缺失时直接失败，不用零值掩盖数据缺口。
     */
    private BigDecimal sumRequired(
            StockCustomCategory category,
            Set<String> stockCodes,
            LocalDate tradeDate,
            Map<LocalDate, Map<String, BigDecimal>> turnovers
    ) {
        // 交易日的股票成交额
        Map<String, BigDecimal> dailyTurnovers = turnovers.getOrDefault(tradeDate, Map.of());
        BigDecimal total = BigDecimal.ZERO;
        for (String stockCode : stockCodes) {
            BigDecimal turnover = dailyTurnovers.get(stockCode);
            if (turnover == null) {
                throw new IllegalStateException(
                        "板块成交额数据不完整，category=" + category.name()
                                + "，tradeDate=" + tradeDate
                                + "，stockCode=" + stockCode
                );
            }
            total = total.add(turnover);
        }
        return total;
    }

    /**
     * 将人民币元转换为亿元，按四舍五入保留两位小数。
     */
    private BigDecimal toYi(BigDecimal amountYuan) {
        return amountYuan.divide(ONE_HUNDRED_MILLION, 2, RoundingMode.HALF_UP);
    }
}
