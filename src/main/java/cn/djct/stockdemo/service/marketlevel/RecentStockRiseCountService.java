package cn.djct.stockdemo.service.marketlevel;

import cn.djct.stockdemo.pojo.dto.RecentStockRiseCountDto;

import java.util.List;

/**
 * 近期股票涨幅家数查询服务。
 */
public interface RecentStockRiseCountService {

    /**
     * 查询最近10个交易日的股票涨幅家数。
     *
     * @return 按交易日升序排列的统计结果
     */
    List<RecentStockRiseCountDto> getLatest();
}
