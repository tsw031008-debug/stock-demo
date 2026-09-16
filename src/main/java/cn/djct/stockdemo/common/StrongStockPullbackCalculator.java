package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 强势股回调公式；输入按T-49至T升序排列且价格已校验的50条日线。 */
@Component
public class StrongStockPullbackCalculator {
    /** 当前板块比例统一应用于历史日线；收盘涨停至少5次且10日振幅严格大于15%。 */
    public boolean matches(List<StockDailyQuote> quotes, BigDecimal limitUpRate) {
        if (quotes.size() != 50) {
            throw new IllegalArgumentException("强势股回调需要50个交易日日线");
        }
        //条件1：最近50日涨停次数>4
        //按照涨停价去 涨停次数
        long count = quotes.stream().filter(quote -> isClosingLimitUp(quote, limitUpRate)).count();
        if (count < 5) {
            return false;
        }
        //条件2：最近10日振幅>15%
        // 最近10日为T-9至T，分母为T-10收盘价；交叉相乘避免舍入改变15%边界。
        BigDecimal high = quotes.get(40).getHighPrice();
        BigDecimal low = quotes.get(40).getLowPrice();
        for (int i = 41; i < 50; i++) {
            high = high.max(quotes.get(i).getHighPrice());
            low = low.min(quotes.get(i).getLowPrice());
        }
        return high.subtract(low).multiply(new BigDecimal("100"))
                .compareTo(quotes.get(39).getClosePrice().multiply(new BigDecimal("15"))) > 0;
    }

    /** 涨停价按分四舍五入；盘中触及但收盘打开不计入。 */
    public boolean isClosingLimitUp(StockDailyQuote quote, BigDecimal limitUpRate) {
        // 计算对应的涨停价
        BigDecimal limitUpPrice = quote.getPreviousClosePrice().multiply(BigDecimal.ONE.add(limitUpRate))
                .setScale(2, RoundingMode.HALF_UP);
        return quote.getClosePrice().compareTo(limitUpPrice) == 0;
    }
}
