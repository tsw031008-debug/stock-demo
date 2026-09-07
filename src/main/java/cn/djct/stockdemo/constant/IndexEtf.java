package cn.djct.stockdemo.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 功能10使用的固定指数ETF，声明顺序即柱状图展示顺序。
 */
@Getter
@RequiredArgsConstructor
public enum IndexEtf {

    SSE_50("510050", "上证50", "sh510050"),
    CSI_300("510300", "沪深300", "sh510300"),
    CHINEXT_50("159949", "创业板50", "sz159949"),
    CSI_1000("512100", "中证1000", "sh512100");

    private final String etfCode;
    private final String indexName;
    private final String tencentSymbol;
}
