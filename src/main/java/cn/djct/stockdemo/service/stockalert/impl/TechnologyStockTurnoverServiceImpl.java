package cn.djct.stockdemo.service.stockalert.impl;

import cn.djct.stockdemo.common.TechnologyStockTurnoverCalculator;
import cn.djct.stockdemo.mapper.StockDailyQuoteMapper;
import cn.djct.stockdemo.pojo.vo.TechnologyStockTurnoverRespVo;
import cn.djct.stockdemo.service.stockalert.TechnologyStockPoolSourceService;
import cn.djct.stockdemo.service.stockalert.TechnologyStockTurnoverService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** 编排指定日期的科技股成交额异动查询。 */
@Service
@RequiredArgsConstructor
public class TechnologyStockTurnoverServiceImpl implements TechnologyStockTurnoverService {
    private final StockDailyQuoteMapper stockDailyQuoteMapper;
    private final TradeCalendarService tradeCalendarService;
    private final TechnologyStockPoolSourceService stockPoolSourceService;
    private final TechnologyStockTurnoverCalculator calculator;

    @Override
    public TechnologyStockTurnoverRespVo findByTradeDate(LocalDate tradeDate) {
        // 参数校验
        if (tradeDate == null) {
            throw new IllegalArgumentException("统计日期不能为空");
        }
        if (tradeDate.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
            throw new IllegalArgumentException("统计日期不能是未来日期");
        }
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalArgumentException("统计日期不是交易日");
        }
        // 仅判断有无落库记录，不将数量当作完整性证明，也不回退到其他日期。
        if (stockDailyQuoteMapper.countByTradeDate(tradeDate) == 0) {
            throw new IllegalStateException("该交易日日线数据尚未就绪，暂无法计算");
        }
        //获取统计日的前三日
        List<LocalDate> dates = List.of(tradeDate,
                tradeCalendarService.getPreviousTradingDay(tradeDate, 1),
                tradeCalendarService.getPreviousTradingDay(tradeDate, 2),
                tradeCalendarService.getPreviousTradingDay(tradeDate, 3));
        //获取科技股候选池
        List<String> codes = stockPoolSourceService.fetchStockCodes();
        if (codes == null || codes.isEmpty()) {
            throw new IllegalStateException("科技股候选池数据为空");
        }
        //根据日期和候选池获取日线数据计算
        return calculator.calculate(dates,
                stockDailyQuoteMapper.selectByStockCodesAndTradeDates(codes, dates));
    }
}
