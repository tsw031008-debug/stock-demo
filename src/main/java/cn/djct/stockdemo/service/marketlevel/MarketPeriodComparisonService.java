package cn.djct.stockdemo.service.marketlevel;

import cn.djct.stockdemo.pojo.vo.MarketPeriodComparisonRespVo;

/**
 * 市场周月平均成交额同比环比服务。
 */
public interface MarketPeriodComparisonService {

    MarketPeriodComparisonRespVo getLatest();
}
