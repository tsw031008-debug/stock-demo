package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockFundFlowDto;
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
    PageDto<StockFundFlowDto> findByTradeDate(
            LocalDate tradeDate,
            int pageNum,
            int pageSize
    );

    int countByTradeDate(LocalDate tradeDate);

    int saveSnapshot(List<StockFundFlow> fundFlows);
}
