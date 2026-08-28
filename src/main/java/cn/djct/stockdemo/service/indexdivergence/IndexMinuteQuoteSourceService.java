package cn.djct.stockdemo.service.indexdivergence;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;

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
}
