package cn.djct.stockdemo.service.stockalert;

import java.util.List;

/**
 * 科技板块候选股票数据源。
 */
public interface TechnologyStockPoolSourceService {

    /**
     * 获取同花顺科技板块的全部股票代码。
     *
     * @return 不重复的六位股票代码
     */
    List<String> fetchStockCodes();
}
