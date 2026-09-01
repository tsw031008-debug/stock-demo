package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.StockPlateLimitUpDto;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import cn.djct.stockdemo.pojo.entity.StockPlateDailyQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 股票板块指标计算组件。
 */
@Component
@RequiredArgsConstructor
public class StockPlateCalculator {

    private static final String DATA_SOURCE = "THS_GN_3_LEVEL";
    private static final int CALCULATION_SCALE = 8;
    private static final int DAILY_SCALE = 4;
    private static final int RESPONSE_SCALE = 2;
    private static final BigDecimal INITIAL_INDEX_VALUE = new BigDecimal("1000");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final StockAlertCalculator stockAlertCalculator;

    /**
     * 计算指定交易日的板块日线。
     *
     * @param tradeDate           交易日
     * @param members             当前有效板块成分关系
     * @param stockQuotes         股票日行情原始数据
     * @param previousClosePrices 上一交易日板块收盘值
     * @param firstTradingDay     是否为当年首个交易日
     * @param requestedStatus     期望数据状态
     * @return 板块日线列表
     */
    public List<StockPlateDailyQuote> calculateDailyQuotes(
            LocalDate tradeDate,
            List<StockPlateMemberDto> members,
            List<StockDailyQuote> stockQuotes,
            Map<Long, BigDecimal> previousClosePrices,
            boolean firstTradingDay,
            String requestedStatus
    ) {
        // 校验计算输入，禁止空值参与金融指标计算
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Objects.requireNonNull(members, "板块成分关系不能为空");
        Objects.requireNonNull(stockQuotes, "股票行情不能为空");
        Objects.requireNonNull(previousClosePrices, "板块前收不能为空");
        Objects.requireNonNull(requestedStatus, "数据状态不能为空");

        // 按板块和股票代码组织原始数据，避免逐板块重复扫描全市场行情
        Map<Long, PlateGroup> plateGroups = groupMembers(members);
        Map<String, StockDailyQuote> quotesByCode = indexQuotes(stockQuotes);
        List<StockPlateDailyQuote> result = new ArrayList<>(plateGroups.size());
        for (PlateGroup plate : plateGroups.values()) {
            List<StockDailyQuote> validQuotes = plate.stockCodes().stream()
                    .map(quotesByCode::get)
                    .filter(Objects::nonNull)
                    .toList();
            // 历史日期可能尚未上市，没有任何行情的板块不生成虚假零值日线
            if (validQuotes.isEmpty()) {
                continue;
            }
            validateDailyFields(plate.plateName(), validQuotes);

            // 板块涨幅使用成分股涨幅算术平均值，成交额使用成分股成交额总和
            BigDecimal changePercent = validQuotes.stream()
                    .map(StockDailyQuote::getChangePercent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(validQuotes.size()), CALCULATION_SCALE, RoundingMode.HALF_UP);
            BigDecimal turnoverAmountYuan = validQuotes.stream()
                    .map(StockDailyQuote::getTurnoverAmountYuan)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 年初首日从1000开始；缺少前收的新增板块同样从1000开始并标记为部分数据
            BigDecimal previousClose = previousClosePrices.get(plate.plateId());
            boolean missingPreviousClose = !firstTradingDay && previousClose == null;
            BigDecimal openPrice = firstTradingDay || missingPreviousClose
                    ? INITIAL_INDEX_VALUE
                    : previousClose;
            BigDecimal closePrice = openPrice.multiply(
                    BigDecimal.ONE.add(changePercent.divide(
                            ONE_HUNDRED,
                            CALCULATION_SCALE,
                            RoundingMode.HALF_UP
                    ))
            );
            result.add(StockPlateDailyQuote.builder()
                    .plateId(plate.plateId())
                    .plateName(plate.plateName())
                    .tradeDate(tradeDate)
                    .openPrice(openPrice.setScale(DAILY_SCALE, RoundingMode.HALF_UP))
                    .closePrice(closePrice.setScale(DAILY_SCALE, RoundingMode.HALF_UP))
                    .changePercent(changePercent.setScale(DAILY_SCALE, RoundingMode.HALF_UP))
                    .turnoverAmountYuan(turnoverAmountYuan.setScale(2, RoundingMode.HALF_UP))
                    .stockCount(validQuotes.size())
                    .dataSource(DATA_SOURCE)
                    .dataStatus(missingPreviousClose ? "PARTIAL" : requestedStatus)
                    .build());
        }
        return result;
    }

    /**
     * 计算当前全部板块的涨停统计并按热度排序。
     *
     * @param members       当前有效板块成分关系
     * @param currentQuotes 当前全市场实时行情
     * @return 排序后的板块涨停统计
     */
    public List<StockPlateLimitUpDto> calculateLimitUpStatistics(
            List<StockPlateMemberDto> members,
            List<StockDailyQuote> currentQuotes
    ) {
        // 当前统计必须使用非空的板块关系和全市场行情
        Objects.requireNonNull(members, "板块成分关系不能为空");
        Objects.requireNonNull(currentQuotes, "当前行情不能为空");
        // 按板块和股票代码组织原始数据，避免逐板块重复扫描全市场行情
        Map<Long, PlateGroup> plateGroups = groupMembers(members);
        // 按股票代码索引行情数据，方便后续快速查找
        Map<String, StockDailyQuote> quotesByCode = indexQuotes(currentQuotes);

        // 每个板块仅计算当天有效股票清单与板块成分股的交集
        List<StockPlateLimitUpDto> result = new ArrayList<>(plateGroups.size());
        for (PlateGroup plate : plateGroups.values()) {
            // 过滤出当前板块的有效行情数据
            List<StockDailyQuote> validQuotes = plate.stockCodes().stream()
                    .map(quotesByCode::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (validQuotes.isEmpty()) {
                continue;
            }
            // 验证股票行情数据的涨跌幅字段是否完整
            validateChangeFields(plate.plateName(), validQuotes);
            // 计算板块涨跌幅：成分股涨跌幅算术平均值
            BigDecimal changePercent = validQuotes.stream()
                    .map(StockDailyQuote::getChangePercent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(validQuotes.size()), CALCULATION_SCALE, RoundingMode.HALF_UP);
            // 计算板块涨停股票占比：涨停股票数除以成分股市值占比
            int limitUpCount = (int) validQuotes.stream()
                    .filter(stockAlertCalculator::isLimitUpCandidate)
                    .count();
            BigDecimal limitUpRatio = BigDecimal.valueOf(limitUpCount)
                    .multiply(ONE_HUNDRED)
                    .divide(BigDecimal.valueOf(validQuotes.size()), RESPONSE_SCALE, RoundingMode.HALF_UP);
            result.add(StockPlateLimitUpDto.builder()
                    .plateName(plate.plateName())
                    .totalStockCount(validQuotes.size())
                    .plateChangePercent(changePercent.setScale(RESPONSE_SCALE, RoundingMode.HALF_UP))
                    .limitUpStockCount(limitUpCount)
                    .limitUpRatio(limitUpRatio)
                    .build());
        }
        // 依次按涨停数、涨停占比、板块涨幅倒序和板块名称升序排列
        return result.stream()
                .sorted(Comparator.comparing(
                                StockPlateLimitUpDto::getLimitUpStockCount,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                StockPlateLimitUpDto::getLimitUpRatio,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                StockPlateLimitUpDto::getPlateChangePercent,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(StockPlateLimitUpDto::getPlateName))
                .toList();
    }

    /**
     * 将成分关系按板块组织，并在单板块内按股票代码去重。
     */
    private Map<Long, PlateGroup> groupMembers(List<StockPlateMemberDto> members) {
        Map<Long, MutablePlateGroup> mutableGroups = new LinkedHashMap<>();
        for (StockPlateMemberDto member : members) {
            if (member == null || member.getPlateId() == null || member.getPlateName() == null
                    || member.getStockCode() == null) {
                throw new IllegalArgumentException("板块成分关系字段不完整");
            }
            //按照板块id获取分组，如果这个板块不存在就添加进集合
            MutablePlateGroup group = mutableGroups.computeIfAbsent(
                    member.getPlateId(),
                    key -> new MutablePlateGroup(member.getPlateName(), new LinkedHashMap<>())
            );
            if (!group.plateName().equals(member.getPlateName())) {
                throw new IllegalArgumentException("同一板块ID对应多个板块名称");
            }
            group.stockCodes().put(member.getStockCode(), Boolean.TRUE);
        }
        Map<Long, PlateGroup> groups = new LinkedHashMap<>();
        mutableGroups.forEach((plateId, group) -> groups.put(
                plateId,
                new PlateGroup(plateId, group.plateName(), List.copyOf(group.stockCodes().keySet()))
        ));
        return groups;
    }

    /**
     * 将行情按股票代码建立唯一索引。
     */
    private Map<String, StockDailyQuote> indexQuotes(List<StockDailyQuote> quotes) {
        Map<String, StockDailyQuote> quotesByCode = new HashMap<>();
        for (StockDailyQuote quote : quotes) {
            if (quote == null || quote.getStockCode() == null) {
                throw new IllegalArgumentException("股票行情缺少股票代码");
            }
            if (quotesByCode.put(quote.getStockCode(), quote) != null) {
                throw new IllegalArgumentException("股票行情存在重复代码：" + quote.getStockCode());
            }
        }
        return quotesByCode;
    }

    /**
     * 校验板块日线计算需要的涨幅和成交额字段。
     */
    private void validateDailyFields(String plateName, List<StockDailyQuote> quotes) {
        validateChangeFields(plateName, quotes);
        if (quotes.stream().anyMatch(quote -> quote.getTurnoverAmountYuan() == null)) {
            throw new IllegalStateException("板块成分股成交额不完整，plateName=" + plateName);
        }
    }

    /**
     * 校验板块平均涨幅计算需要的涨幅字段。
     */
    private void validateChangeFields(String plateName, List<StockDailyQuote> quotes) {
        if (quotes.stream().anyMatch(quote -> quote.getChangePercent() == null)) {
            throw new IllegalStateException("板块成分股涨幅不完整，plateName=" + plateName);
        }
    }

    /**
     * 不可变板块分组。
     */
    private record PlateGroup(Long plateId, String plateName, List<String> stockCodes) {
    }

    /**
     * 板块成分关系去重过程中的可变分组。
     */
    private record MutablePlateGroup(String plateName, Map<String, Boolean> stockCodes) {
    }
}
