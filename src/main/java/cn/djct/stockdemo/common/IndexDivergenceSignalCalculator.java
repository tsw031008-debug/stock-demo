package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexDivergenceSignalType;
import cn.djct.stockdemo.pojo.vo.IndexDivergenceSignalRespVo;
import cn.djct.stockdemo.pojo.dto.IndexMacdDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 指数MACD顶背离和底背离计算组件。
 */
@Component
public class IndexDivergenceSignalCalculator {

    private static final int MINIMUM_VALID_MIDDLE_KLINE_COUNT = 3;

    /**
     * 根据按分钟升序排列的MACD结果识别全部背离信号。
     */
    public List<IndexDivergenceSignalRespVo> detect(List<IndexMacdDto> macdItems) {
        Objects.requireNonNull(macdItems, "MACD结果不能为空");
        if (macdItems.size() < 2) {
            //保证计算信号最少两个k线个数
            return List.of();
        }
        validateItems(macdItems);

        //找到所有金叉和死叉的交叉点
        List<CrossPoint> crosses = findCrosses(macdItems);
        //用于存储所有顶背离和底背离候选区间
        List<DivergenceInterval> topIntervals = new ArrayList<>();
        List<DivergenceInterval> bottomIntervals = new ArrayList<>();
        //遍历所有交叉点对，找到所有顶背离和底背离区间
        for (int index = 1; index < crosses.size(); index++) {
            CrossPoint start = crosses.get(index - 1);
            CrossPoint end = crosses.get(index);
            //计算两个交叉点之间的K线数量
            int middleKlineCount = end.itemIndex - start.itemIndex - 1;
            if (middleKlineCount < MINIMUM_VALID_MIDDLE_KLINE_COUNT) {
                continue;
            }
            if (start.type == CrossType.DEATH && end.type == CrossType.GOLDEN) {
                //死叉在前，金叉在后，表示底背离 false,
                //底背离候选，根据两个交叉点划定一个有效周期，并计算这个周期内价格、MACD和DIF的极值。
                bottomIntervals.add(buildInterval(macdItems, start.itemIndex, end.itemIndex, false));
            } else if (start.type == CrossType.GOLDEN && end.type == CrossType.DEATH) {
                //金叉在前，死叉在后，表示顶背离 true,
                //顶背离候选，根据两个交叉点划定一个有效周期，并计算这个周期内价格、MACD和DIF的极值。
                topIntervals.add(buildInterval(macdItems, start.itemIndex, end.itemIndex, true));
            }
        }

        List<IndexDivergenceSignalRespVo> signals = new ArrayList<>();
        //将所有符合条件的顶背离和底背离信号添加到结果列表中
        appendBottomSignals(bottomIntervals, signals);
        appendTopSignals(topIntervals, signals);
        //按信号时间升序排序结果列表
        signals.sort(Comparator.comparing(IndexDivergenceSignalRespVo::getSignalTime));
        return List.copyOf(signals);
    }

    /**
     * 严格比较相邻分钟，DIF等于DEA时不产生交叉。
     */
    private List<CrossPoint> findCrosses(List<IndexMacdDto> macdItems) {
        List<CrossPoint> crosses = new ArrayList<>();
        //遍历所有K线，找到所有金叉和死叉的交叉点
        for (int index = 1; index < macdItems.size(); index++) {
            //比较相邻K线的DIF和DEA的大小关系
            int previousRelation = macdItems.get(index - 1).getDif()
                    .compareTo(macdItems.get(index - 1).getDea());
            //当前K线的DIF和DEA的大小关系
            int currentRelation = macdItems.get(index).getDif()
                    .compareTo(macdItems.get(index).getDea());
            //DIF和DEA的大小关系从小于变为大于，表示 金叉
            if (previousRelation < 0 && currentRelation > 0) {
                crosses.add(new CrossPoint(index, CrossType.GOLDEN));
            } else if (previousRelation > 0 && currentRelation < 0) {//DIF和DEA的大小关系从大于变为小于，表示 死叉
                crosses.add(new CrossPoint(index, CrossType.DEATH));
            }
        }
        return crosses;
    }

    /**
     * 区间包含起止交叉分钟；顶背离取最高值，底背离取最低值。
     */
    private DivergenceInterval buildInterval(
            List<IndexMacdDto> macdItems,
            int startIndex,
            int endIndex,
            boolean maximum
    ) {
        //获取区间起始K线
        IndexMacdDto first = macdItems.get(startIndex);
        BigDecimal priceExtreme = first.getCurrentPrice();
        BigDecimal macdExtreme = first.getMacd();
        BigDecimal difExtreme = first.getDif();
        for (int index = startIndex + 1; index <= endIndex; index++) {
            //获取当前K线
            IndexMacdDto item = macdItems.get(index);
            //更新区间价格极值
            priceExtreme = selectExtreme(priceExtreme, item.getCurrentPrice(), maximum);
            //更新区间MACD极值
            macdExtreme = selectExtreme(macdExtreme, item.getMacd(), maximum);
            //更新区间DIF极值
            difExtreme = selectExtreme(difExtreme, item.getDif(), maximum);
        }
        return new DivergenceInterval(
                first.getQuoteTime(),
                macdItems.get(endIndex).getQuoteTime(),
                priceExtreme,
                macdExtreme,
                difExtreme
        );
    }

    /**
     * 在已有极值和候选值中选择新的极值。
     *
     * @param current 当前已找到的极值
     * @param candidate 新的候选值
     * @param maximum true取最大值，false取最小值
     * @return 新的极值
     */
    private BigDecimal selectExtreme(BigDecimal current, BigDecimal candidate, boolean maximum) {
        //比较当前极值和候选值
        int comparison = candidate.compareTo(current);
        //如果需要最大值，则返回较大的值，否则返回较小的值
        if (maximum) {
            //如果候选值大于当前极值，则返回候选值，否则返回当前极值
            return comparison > 0 ? candidate : current;
        }
        //如果候选值小于当前极值，则返回候选值，否则返回当前极值
        return comparison < 0 ? candidate : current;
    }

    private void appendBottomSignals(
            List<DivergenceInterval> intervals,
            List<IndexDivergenceSignalRespVo> signals
    ) {
        //遍历所有底背离候选区间
        for (int index = 1; index < intervals.size(); index++) {
            //获取前一个和当前底背离候选区间
            DivergenceInterval previous = intervals.get(index - 1);
            DivergenceInterval current = intervals.get(index);
            //1.前一个周期的最低价格高于当前周期
            //2.前一个周期的MACD值低于当前周期
            //3.前一个周期的DIF值低于当前周期
            if (previous.priceExtreme.compareTo(current.priceExtreme) > 0
                    && previous.macdExtreme.compareTo(current.macdExtreme) < 0
                    && previous.difExtreme.compareTo(current.difExtreme) < 0) {
                signals.add(toSignal(IndexDivergenceSignalType.MACD_BOTTOM, previous, current));
            }
        }
    }

    private void appendTopSignals(
            List<DivergenceInterval> intervals,
            List<IndexDivergenceSignalRespVo> signals
    ) {
        //遍历所有顶背离候选区间
        for (int index = 1; index < intervals.size(); index++) {
            //获取前一个和当前顶背离候选区间
            DivergenceInterval previous = intervals.get(index - 1);
            DivergenceInterval current = intervals.get(index);
            //1.前一个周期的最高价格低于当前周期
            //2.前一个周期的MACD值高于当前周期
            //3.前一个周期的DIF值高于当前周期
            if (previous.priceExtreme.compareTo(current.priceExtreme) < 0
                    && previous.macdExtreme.compareTo(current.macdExtreme) > 0
                    && previous.difExtreme.compareTo(current.difExtreme) > 0) {
                signals.add(toSignal(IndexDivergenceSignalType.MACD_TOP, previous, current));
            }
        }
    }

    private IndexDivergenceSignalRespVo toSignal(
            IndexDivergenceSignalType signalType,
            DivergenceInterval previous,
            DivergenceInterval current
    ) {
        return IndexDivergenceSignalRespVo.builder()
                .signalType(signalType)
                .signalTime(current.endTime)
                .previousIntervalStartTime(previous.startTime)
                .previousIntervalEndTime(previous.endTime)
                .currentIntervalStartTime(current.startTime)
                .currentIntervalEndTime(current.endTime)
                .previousPriceExtreme(previous.priceExtreme)
                .currentPriceExtreme(current.priceExtreme)
                .previousMacdExtreme(previous.macdExtreme)
                .currentMacdExtreme(current.macdExtreme)
                .previousDifExtreme(previous.difExtreme)
                .currentDifExtreme(current.difExtreme)
                .build();
    }

    private void validateItems(List<IndexMacdDto> macdItems) {
        LocalDateTime previousTime = null;
        for (IndexMacdDto item : macdItems) {
            if (item == null || item.getQuoteTime() == null || item.getCurrentPrice() == null
                    || item.getDif() == null || item.getDea() == null || item.getMacd() == null) {
                throw new IllegalArgumentException("MACD结果字段不完整");
            }
            if (previousTime != null && !item.getQuoteTime().isAfter(previousTime)) {
                throw new IllegalArgumentException("MACD结果必须按时间严格升序排列");
            }
            previousTime = item.getQuoteTime();
        }
    }

    private enum CrossType {
        GOLDEN,
        DEATH
    }

    private record CrossPoint(int itemIndex, CrossType type) {
    }

    private record DivergenceInterval(
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal priceExtreme,
            BigDecimal macdExtreme,
            BigDecimal difExtreme
    ) {
    }
}
