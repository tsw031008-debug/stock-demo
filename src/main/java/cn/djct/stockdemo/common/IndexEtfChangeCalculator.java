package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.IndexEtf;
import cn.djct.stockdemo.pojo.dto.IndexEtfChangeDto;
import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指数ETF涨幅计算组件。
 */
@Component
public class IndexEtfChangeCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /**
     * 使用统计日与前第5个交易日的收盘价计算涨幅，结果单位为百分比。
     *
     * @param baseTradeDate 统计日前第5个交易日
     * @param statisticsDate 统计交易日
     * @param quotes 两个交易日的四只固定ETF日行情
     * @return 按产品固定顺序排列的ETF涨幅
     */
    public List<IndexEtfChangeDto> calculate(
            LocalDate baseTradeDate,
            LocalDate statisticsDate,
            List<IndexEtfDailyQuote> quotes
    ) {
        if (quotes == null) {
            throw new IllegalArgumentException("指数ETF日行情不能为空");
        }
        Map<QuoteKey, IndexEtfDailyQuote> quoteByKey = new HashMap<>();
        // 遍历行情数据，按交易日与指数代码分组
        for (IndexEtfDailyQuote quote : quotes) {
            // 验证行情数据
            validateQuote(baseTradeDate, statisticsDate, quote);
            QuoteKey key = new QuoteKey(quote.getTradeDate(), quote.getEtfCode());
            // 插入行情数据
            if (quoteByKey.put(key, quote) != null) {
                throw new IllegalStateException("指数ETF日行情重复，tradeDate="
                        + quote.getTradeDate() + "，etfCode=" + quote.getEtfCode());
            }
        }

        List<IndexEtfChangeDto> result = new ArrayList<>(IndexEtf.values().length);
        for (IndexEtf etf : IndexEtf.values()) {
            // 查询指数ETF的基准日和统计日价格完整日行情。
            BigDecimal basePrice = requiredPrice(quoteByKey, baseTradeDate, etf);
            BigDecimal currentPrice = requiredPrice(quoteByKey, statisticsDate, etf);
            // 5日涨幅 = (统计日收盘价 / 前第5个交易日收盘价 - 1) * 100%。
            BigDecimal changePercent = currentPrice
                    .divide(basePrice, 10, RoundingMode.HALF_UP)
                    .subtract(BigDecimal.ONE)
                    .multiply(ONE_HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(IndexEtfChangeDto.builder()
                    .etfCode(etf.getEtfCode())
                    .indexName(etf.getIndexName())
                    .changePercent(changePercent)
                    .build());
        }
        return result;
    }

    private void validateQuote(
            LocalDate baseTradeDate,
            LocalDate statisticsDate,
            IndexEtfDailyQuote quote
    ) {
        if (quote == null || quote.getTradeDate() == null || quote.getEtfCode() == null
                || quote.getClosePrice() == null) {
            throw new IllegalStateException("指数ETF日行情字段不完整");
        }
        if (!baseTradeDate.equals(quote.getTradeDate())
                && !statisticsDate.equals(quote.getTradeDate())) {
            throw new IllegalStateException("指数ETF日行情日期超出计算范围，tradeDate="
                    + quote.getTradeDate());
        }
        if (quote.getClosePrice().signum() <= 0) {
            throw new IllegalStateException("指数ETF收盘价必须大于0，etfCode="
                    + quote.getEtfCode());
        }
    }

    private BigDecimal requiredPrice(
            Map<QuoteKey, IndexEtfDailyQuote> quoteByKey,
            LocalDate tradeDate,
            IndexEtf etf
    ) {
        IndexEtfDailyQuote quote = quoteByKey.get(new QuoteKey(tradeDate, etf.getEtfCode()));
        if (quote == null) {
            throw new IllegalStateException("指数ETF日行情缺失，tradeDate=" + tradeDate
                    + "，etfCode=" + etf.getEtfCode());
        }
        return quote.getClosePrice();
    }

    private record QuoteKey(LocalDate tradeDate, String etfCode) {
    }
}
