package cn.djct.stockdemo.service.marketlevel;

import java.math.BigDecimal;

/**
 * 市场水位实时数据源服务。
 */
public interface MarketLevelSourceService {

    /**
     * 获取当前沪深两市成交额，单位：元。
     *
     * @return 当前沪深两市成交额
     */
    BigDecimal fetchCurrentTurnoverAmountYuan();
}
