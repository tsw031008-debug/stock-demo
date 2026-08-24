package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.pojo.entity.StockFundFlow;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票资金流向持久化服务。
 */
public interface StockFundFlowService {

    int countByTradeDate(LocalDate tradeDate);

    int saveSnapshot(List<StockFundFlow> fundFlows);
}
