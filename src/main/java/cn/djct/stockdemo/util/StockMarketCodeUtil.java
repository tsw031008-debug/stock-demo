package cn.djct.stockdemo.util;

/**
 * 股票代码市场前缀转换。
 */
public final class StockMarketCodeUtil {

    private StockMarketCodeUtil() {
    }

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
