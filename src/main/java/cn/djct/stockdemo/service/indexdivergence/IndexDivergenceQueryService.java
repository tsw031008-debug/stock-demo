package cn.djct.stockdemo.service.indexdivergence;

import cn.djct.stockdemo.pojo.vo.IndexDivergenceLatestRespVo;

/**
 * 指数曲线和背离信号查询服务。
 */
public interface IndexDivergenceQueryService {

    /**
     * 查询最新交易日的上证指数分钟曲线和背离信号。
     */
    IndexDivergenceLatestRespVo getLatest();
}
