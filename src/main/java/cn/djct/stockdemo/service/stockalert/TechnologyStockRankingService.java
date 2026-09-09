package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.pojo.vo.TechnologyStockRankingRespVo;

/**
 * 热门和潜力科技股榜单查询服务。
 */
public interface TechnologyStockRankingService {

    /**
     * 查询日行情表最新交易日的科技股榜单。
     *
     * @return 热门和潜力科技股前五名
     */
    TechnologyStockRankingRespVo getLatest();
}
