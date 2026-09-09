package cn.djct.stockdemo.service.marketlevel;

import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;

/**
 * 市场水位服务。
 */
public interface MarketLevelService {

    /**
     * 查询最新市场水位。
     *
     * @return 最新市场水位
     */
    MarketLevelRespVo getLatest();
}
