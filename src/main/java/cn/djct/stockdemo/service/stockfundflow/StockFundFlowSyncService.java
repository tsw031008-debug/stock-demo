package cn.djct.stockdemo.service.stockfundflow;

import java.time.LocalDate;

/**
 * 股票资金流向同步服务。
 */
public interface StockFundFlowSyncService {

    /**
     * 同步股票资金流向数据。
     * @param tradeDate 交易日
     * @return 影响行数
     */
    int synchronize(LocalDate tradeDate);
}
