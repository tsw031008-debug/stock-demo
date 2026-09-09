package cn.djct.stockdemo.common;

import cn.djct.stockdemo.constant.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局接口异常处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingRequestParameter(MissingServletRequestParameterException exception) {
        return Result.error(ResultCode.OPERATION_ERROR, "请求参数不能为空：" + exception.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleRequestParameterTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = java.time.LocalDate.class.equals(exception.getRequiredType())
                ? "日期格式必须为yyyy-MM-dd：" + exception.getName()
                : "请求参数格式错误：" + exception.getName();
        return Result.error(ResultCode.OPERATION_ERROR, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("请求参数错误");

        return Result.error(ResultCode.OPERATION_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable() {
        return Result.error(ResultCode.OPERATION_ERROR, "请求体不能为空或格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return Result.error(ResultCode.OPERATION_ERROR, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(
            IllegalStateException exception
    ) {
        return Result.error(ResultCode.OPERATION_ERROR, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("接口处理发生未预期异常", exception);
        return Result.error(ResultCode.SYSTEM_ERROR, "系统异常");
    }
}
