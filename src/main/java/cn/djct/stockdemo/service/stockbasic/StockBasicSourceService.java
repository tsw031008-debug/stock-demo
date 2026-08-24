package cn.djct.stockdemo.service.stockbasic;

import cn.djct.stockdemo.pojo.dto.StockBasicDto;

import java.util.List;

/**
 * 股票基础信息数据源。
 */
public interface StockBasicSourceService {

    /**
     * 获取数据源返回的全部沪深京A股。
     */
    List<StockBasicDto> fetchAll();
}
