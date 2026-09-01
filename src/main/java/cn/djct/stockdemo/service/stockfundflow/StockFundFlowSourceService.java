package cn.djct.stockdemo.service.stockfundflow;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票资金流向数据源服务。
 */
public interface StockFundFlowSourceService {

    List<StockFundFlow> fetchAll(LocalDate tradeDate, List<StockBasic> stocks);
}
