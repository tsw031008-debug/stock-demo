package cn.djct.stockdemo.service.plate;

import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverComparisonDto;

/**
 * 四大类当前与上一交易日成交额对比服务。
 */
public interface StockCategoryTurnoverComparisonService {

    /**
     * 查询当前交易日及上一交易日的四大类成交额。
     *
     * 当前交易日只实时查询四大类涉及的股票，上一交易日成交额从日行情表读取。
     * 同一大类内重复股票只计算一次，单位为亿元。
     *
     * @return 四大类两个交易日的成交额对比
     */
    StockCategoryTurnoverComparisonDto getCurrent();
}
