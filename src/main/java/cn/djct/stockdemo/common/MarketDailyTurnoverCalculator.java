package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.dto.MarketTurnoverRecordDto;
import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 每日沪深市场成交额汇总组件。
 */
@Component
public class MarketDailyTurnoverCalculator {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 汇总一个交易日全部沪深A股的成交额。
     *
     * @param tradeDate 交易日
     * @param records   当日逐股成交额原始记录
     * @return 每日市场成交额汇总
     */
    public MarketDailyTurnover calculate(LocalDate tradeDate, List<MarketTurnoverRecordDto> records) {
        Objects.requireNonNull(tradeDate, "交易日不能为空");
        Objects.requireNonNull(records, "市场成交额明细不能为空");
        if (records.isEmpty()) {
            throw new IllegalStateException("市场成交额明细为空，tradeDate=" + tradeDate);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (MarketTurnoverRecordDto record : records) {
            // 任意记录跨日期、成交额为空或为负数时，都不能生成COMPLETE汇总
            if (record == null || !tradeDate.equals(record.getTradeDate())) {
                throw new IllegalStateException("市场成交额明细交易日不一致，tradeDate=" + tradeDate);
            }
            if (record.getTurnoverAmountYuan() == null || record.getTurnoverAmountYuan().signum() < 0) {
                throw new IllegalStateException("市场成交额明细不完整，tradeDate=" + tradeDate);
            }
            total = total.add(record.getTurnoverAmountYuan());
        }

        // 校验通过后，股票数量与有效成交额记录数量必然一致
        return MarketDailyTurnover.builder()
                .tradeDate(tradeDate)
                .turnoverAmountYuan(total)
                .stockCount(records.size())
                .amountRecordCount(records.size())
                .dataSource("STOCK_DAILY_QUOTE")
                .dataStatus("COMPLETE")
                .calculatedAt(LocalDateTime.now(SHANGHAI_ZONE))
                .build();
    }
}
