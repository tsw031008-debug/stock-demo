package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.DailyMarketTurnoverDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 市场水位计算组件，历史数据按交易日倒序传入且不包含当前统计日。
 */
@Component
public class MarketLevelCalculator {

    private static final int REQUIRED_HISTORY_DAYS = 5;
    private static final int STYLE_HISTORY_DAYS = 3;
    private static final int RESPONSE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED_MILLION = new BigDecimal("100000000");
    private static final BigDecimal EIGHT_THOUSAND_YI = new BigDecimal("8000")
            .multiply(ONE_HUNDRED_MILLION);
    private static final BigDecimal TEN_THOUSAND_YI = new BigDecimal("10000")
            .multiply(ONE_HUNDRED_MILLION);

    /**
     * 计算最新市场水位。
     *
     * @param currentTurnoverAmountYuan 当前两市成交额，单位：元
     * @param historicalTurnovers       统计日前5个交易日成交额，按交易日倒序排列
     * @return 市场水位计算结果
     */
    public MarketLevelRespVo calculate(
            BigDecimal currentTurnoverAmountYuan,
            List<DailyMarketTurnoverDto> historicalTurnovers
    ) {
        // 校验当前成交额和历史成交额完整性
        validateCurrentTurnover(currentTurnoverAmountYuan);
        validateHistoricalTurnovers(historicalTurnovers);

        // 最近3个交易日成交额用于判断市场风格
        BigDecimal previousThreeDayTotal = historicalTurnovers.stream()
                .limit(STYLE_HISTORY_DAYS)
                .map(DailyMarketTurnoverDto::getTurnoverAmountYuan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 最近5个交易日成交额用于计算量比
        BigDecimal previousFiveDayTotal = historicalTurnovers.stream()
                .map(DailyMarketTurnoverDto::getTurnoverAmountYuan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (previousFiveDayTotal.signum() == 0) {
            throw new IllegalStateException("过去5个交易日成交额均值不能为0");
        }

        // 使用未舍入金额完成风格和量比计算，最终结果统一保留2位小数
        return MarketLevelRespVo.builder()
                //风格
                .style(determineStyle(previousThreeDayTotal))
                //前3日成交额均值
                .previousThreeDayAverageTurnoverYi(previousThreeDayTotal
                        // 3个交易日均值
                        .divide(BigDecimal.valueOf(STYLE_HISTORY_DAYS), 8, RoundingMode.HALF_UP)
                        // 亿
                        .divide(ONE_HUNDRED_MILLION, RESPONSE_SCALE, RoundingMode.HALF_UP))
                //当前最新两市成交额
                .currentTurnoverYi(currentTurnoverAmountYuan
                        // 亿
                        .divide(ONE_HUNDRED_MILLION, RESPONSE_SCALE, RoundingMode.HALF_UP))
                //量比
                .volumeRatio(currentTurnoverAmountYuan
                        // 5个交易日均值
                        .multiply(BigDecimal.valueOf(REQUIRED_HISTORY_DAYS))
                        // 亿
                        .divide(previousFiveDayTotal, RESPONSE_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 根据前3个交易日成交额总和判断市场风格。
     *
     * @param previousThreeDayTotal 前3个交易日成交额总和，单位：元
     * @return 市场风格
     */
    private String determineStyle(BigDecimal previousThreeDayTotal) {
        // 8000亿*3=24000亿
        BigDecimal lowerBoundaryTotal = EIGHT_THOUSAND_YI.multiply(BigDecimal.valueOf(STYLE_HISTORY_DAYS));
        // 10000亿*3=30000亿
        BigDecimal upperBoundaryTotal = TEN_THOUSAND_YI.multiply(BigDecimal.valueOf(STYLE_HISTORY_DAYS));
        if (previousThreeDayTotal.compareTo(lowerBoundaryTotal) < 0) {
            return "偏游资风格";
        }
        if (previousThreeDayTotal.compareTo(upperBoundaryTotal) <= 0) {
            return "过渡期";
        }
        return "偏机构风格";
    }

    /**
     * 校验当前两市成交额。
     *
     * @param currentTurnoverAmountYuan 当前两市成交额，单位：元
     */
    private void validateCurrentTurnover(BigDecimal currentTurnoverAmountYuan) {
        Objects.requireNonNull(currentTurnoverAmountYuan, "当前两市成交额不能为空");
        if (currentTurnoverAmountYuan.signum() < 0) {
            throw new IllegalArgumentException("当前两市成交额不能小于0");
        }
    }

    /**
     * 校验历史成交额数量、完整性和日期顺序。
     *
     * @param historicalTurnovers 历史成交额列表
     */
    private void validateHistoricalTurnovers(List<DailyMarketTurnoverDto> historicalTurnovers) {
        Objects.requireNonNull(historicalTurnovers, "历史市场成交额不能为空");
        if (historicalTurnovers.size() != REQUIRED_HISTORY_DAYS) {
            throw new IllegalStateException("历史市场成交额不足5个交易日，actual=" + historicalTurnovers.size());
        }

        LocalDate previousDate = null;
        for (DailyMarketTurnoverDto turnover : historicalTurnovers) {
            if (turnover == null || turnover.getTradeDate() == null || turnover.getTurnoverAmountYuan() == null) {
                throw new IllegalStateException("历史市场成交额存在空数据");
            }
            if (turnover.getTurnoverAmountYuan().signum() < 0) {
                throw new IllegalStateException("历史市场成交额不能小于0，tradeDate=" + turnover.getTradeDate());
            }
            if (turnover.getTotalRecordCount() == null
                    || turnover.getAmountRecordCount() == null
                    || turnover.getTotalRecordCount() <= 0
                    || !turnover.getTotalRecordCount().equals(turnover.getAmountRecordCount())) {
                throw new IllegalStateException("历史市场成交额数据不完整，tradeDate=" + turnover.getTradeDate());
            }
            if (previousDate != null && !turnover.getTradeDate().isBefore(previousDate)) {
                throw new IllegalStateException("历史市场成交额必须按交易日倒序排列");
            }
            previousDate = turnover.getTradeDate();
        }
    }
}
