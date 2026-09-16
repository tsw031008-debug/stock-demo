package cn.djct.stockdemo.util;

import java.math.BigDecimal;

/**
 * 股票代码市场前缀转换。
 */
public final class StockMarketCodeUtil {

    private StockMarketCodeUtil() {
    }

    public static boolean isShanghaiOrShenzhenAStock(String stockCode) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            return false;
        }
        return switch (stockCode.charAt(0)) {
            case '0', '3', '6' -> true;
            default -> false;
        };
    }

    /** 当前非ST A股的涨停比例；调用方负责排除当前ST，不追溯历史状态。 */
    public static BigDecimal currentNonStLimitUpRate(String stockCode) {
        String symbol = toTencentSymbol(stockCode);
        if (symbol.startsWith("bj")) {
            return new BigDecimal("0.30");
        }
        if (stockCode.startsWith("3") || stockCode.startsWith("688") || stockCode.startsWith("689")) {
            return new BigDecimal("0.20");
        }
        return new BigDecimal("0.10");
    }

    // 腾讯股票代码转换
    public static String toTencentSymbol(String stockCode) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("股票代码格式错误：" + stockCode);
        }
        return switch (stockCode.charAt(0)) {
            case '6' -> "sh" + stockCode;
            case '0', '3' -> "sz" + stockCode;
            case '4', '8', '9' -> "bj" + stockCode;
            default -> throw new IllegalArgumentException("无法识别股票市场：" + stockCode);
        };
    }
}
