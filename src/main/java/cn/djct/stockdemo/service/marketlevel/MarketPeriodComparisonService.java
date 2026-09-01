package cn.djct.stockdemo.service.marketlevel;

import cn.djct.stockdemo.pojo.dto.MarketPeriodComparisonDto;

/**
 * 市场周月平均成交额同比环比服务。
 */
public interface MarketPeriodComparisonService {

    MarketPeriodComparisonDto getLatest();
}
