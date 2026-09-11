package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo;
import cn.djct.stockdemo.pojo.vo.PageRespVo;

import java.time.LocalDate;

/** 平台突破盘后选股及历史查询。 */
public interface PlatformBreakoutService {
    /** 按当日基础快照计算并原子替换结果，非交易日跳过；返回入选数量。 */
    int selectStocks(LocalDate tradeDate);

    /** 查询指定交易日最后一次成功的选股结果，未成功执行时抛出状态异常。 */
    PageRespVo<LeftSideStockRespVo> findByTradeDate(LocalDate tradeDate, int pageNum, int pageSize);
}
