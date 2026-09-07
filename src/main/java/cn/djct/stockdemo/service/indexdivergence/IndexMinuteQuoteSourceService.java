package cn.djct.stockdemo.service.indexdivergence;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;

import java.util.List;

/**
 * 指数分钟行情外部数据源。
 */
public interface IndexMinuteQuoteSourceService {

    /**
     * 获取上证指数当前行情。
     *
     * @return 上证指数当前行情
     */
    IndexMinuteQuote fetchShanghaiComposite();

    /**
     * 获取腾讯最近交易日的上证指数完整分时行情。
     *
     * @return 按行情时间升序排列的分时行情
     */
    List<IndexMinuteQuote> fetchShanghaiCompositeMinutes();
}
