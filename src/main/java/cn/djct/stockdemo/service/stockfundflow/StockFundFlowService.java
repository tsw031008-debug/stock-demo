package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockFundFlowRespVo;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票资金流向服务。
 */
public interface StockFundFlowService {

    /**
     * 分页查询指定交易日的资金流向。
     *
     * @param tradeDate 交易日
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 资金流向分页数据
     */
    PageRespVo<StockFundFlowRespVo> findByTradeDate(
            LocalDate tradeDate,
            int pageNum,
            int pageSize
    );

    int countByTradeDate(LocalDate tradeDate);

    /** 判断股票集合是否全部具有当天15:20之后采集的完整资金流向。 */
    boolean hasClosingSnapshot(LocalDate tradeDate, List<String> stockCodes);

    int saveSnapshot(List<StockFundFlow> fundFlows);
}
