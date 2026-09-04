package cn.djct.stockdemo.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 指数大小风格使用的固定指数，声明顺序即接口展示顺序。
 */
@Getter
@RequiredArgsConstructor
public enum IndexStyleIndex {

    SSE_50("000016", "上证50", "sh000016"),
    SHANGHAI_COMPOSITE("000001", "上证指数", "sh000001"),
    CHINEXT_COMPOSITE("399102", "创业板综指", "sz399102"),
    CSI_1000("000852", "中证1000", "sh000852");

    private final String indexCode;
    private final String indexName;
    private final String tencentSymbol;

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
