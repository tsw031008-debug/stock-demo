package cn.djct.stockdemo.constant;

/**
 * 接口响应码。
 */
public final class ResultCode {

    public static final int SUCCESS = 200;

    public static final int OPERATION_ERROR = 1000;

    public static final int SYSTEM_ERROR = 500;

    public static final int FORBIDDEN = 403;

    public static final int UNAUTHORIZED = 401;

    public static final int WEAK_PASSWORD = 400;

    private ResultCode() {
    }
}
