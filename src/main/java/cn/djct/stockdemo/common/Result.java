package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 与前端交互的数据结构。
 *
 * @param <T> 携带数据的类型
 */
@Data
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS, message, data);
    }

    public static <T> Result<T> error(int resultCode, String message) {
        return new Result<>(resultCode, message, null);
    }
}
