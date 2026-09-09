package cn.djct.stockdemo.service.plate;

import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockPlateLimitUpRespVo;

/**
 * 股票板块查询服务。
 */
public interface StockPlateQueryService {

    /**
     * 分页查询当天最新板块涨停统计。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 板块涨停统计分页数据
     */
    PageRespVo<StockPlateLimitUpRespVo> findLimitUpStatistics(int pageNum, int pageSize);
}
