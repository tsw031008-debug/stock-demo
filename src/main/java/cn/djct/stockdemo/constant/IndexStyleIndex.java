package cn.djct.stockdemo.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 日行情采集使用的指数，风格展示范围由styleValues限定。
 */
@Getter
@RequiredArgsConstructor
public enum IndexStyleIndex {

    SSE_50("000016", "上证50", "sh000016"),
    SHANGHAI_COMPOSITE("000001", "上证指数", "sh000001"),
    CHINEXT_COMPOSITE("399102", "创业板综指", "sz399102"),
    CSI_1000("000852", "中证1000", "sh000852"),
    CSI_300("000300", "沪深300", "sh000300");

    private final String indexCode;
    private final String indexName;
    private final String tencentSymbol;

    /** 功能9固定展示四个指数，沪深300仅用于行情采集。 */
    public static IndexStyleIndex[] styleValues() {
        return new IndexStyleIndex[]{SSE_50, SHANGHAI_COMPOSITE, CHINEXT_COMPOSITE, CSI_1000};
    }

    /**
     * 根据指数代码获取固定指数定义。
     *
     * @param indexCode 指数代码
     * @return 固定指数定义
     */
    public static IndexStyleIndex fromCode(String indexCode) {
        return Arrays.stream(values())
                .filter(index -> index.indexCode.equals(indexCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知指数代码：" + indexCode));
    }
}
