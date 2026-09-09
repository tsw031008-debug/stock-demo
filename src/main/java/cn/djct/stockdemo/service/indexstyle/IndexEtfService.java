package cn.djct.stockdemo.service.indexstyle;

import cn.djct.stockdemo.pojo.vo.IndexEtfComparisonRespVo;

/**
 * 指数ETF涨幅查询服务。
 */
public interface IndexEtfService {

    /**
     * 使用最新完整日行情查询四只固定指数ETF的5日涨幅。
     *
     * @return ETF涨幅比较结果
     */
    IndexEtfComparisonRespVo getLatest();
}
