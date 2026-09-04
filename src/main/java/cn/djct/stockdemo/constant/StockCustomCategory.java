package cn.djct.stockdemo.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 四大自定义板块分类，声明顺序同时作为接口展示顺序。
 */
@Getter
@RequiredArgsConstructor
public enum StockCustomCategory {

    CYCLE("周期"),
    FINANCE("金融"),
    TECHNOLOGY("科技"),
    CONSUMPTION("消费");

    private final String displayName;

    /**
     * 根据分类编码获取枚举。
     *
     * @param code 分类编码
     * @return 分类枚举
     */
    public static StockCustomCategory fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("板块大类编码不能为空");
        }
        return Arrays.stream(values())
                .filter(category -> category.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知板块大类编码：" + code));
    }
}
