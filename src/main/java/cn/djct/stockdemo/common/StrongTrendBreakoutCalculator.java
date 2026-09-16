package cn.djct.stockdemo.common;

import cn.djct.stockdemo.pojo.entity.StockDailyQuote;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

/** 强势趋势突破：此前最多128日最高价，不包含今天；历史短时使用已有连续行情。 */
@Component
public class StrongTrendBreakoutCalculator {
    /** 输入含今日的61至129条升序有效日线；收盘突破与涨幅5%均使用严格大于。 */
    public boolean matches(List<StockDailyQuote> quotes) {
        if (quotes.size() < 61 || quotes.size() > 129) {
            throw new IllegalArgumentException("强势趋势突破需要含今日61至129条连续日线");
        }
        StockDailyQuote today = quotes.get(quotes.size() - 1);
        //找到最近128日的最高价的最大值
        BigDecimal lastHigh = quotes.get(0).getHighPrice();
        for (int i = 1; i < quotes.size() - 1; i++) {
            lastHigh = lastHigh.max(quotes.get(i).getHighPrice());
        }
        return today.getClosePrice().compareTo(lastHigh) > 0
                && today.getChangePercent().compareTo(new BigDecimal("5")) > 0;
    }
}
