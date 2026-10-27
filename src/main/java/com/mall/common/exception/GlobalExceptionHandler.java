package com.mall.common.exception;

import com.mall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将业务、参数校验、权限和未预期异常统一转换为 API 响应。 */
/**
 * 全局异常处理器
 * 使用@RestControllerAdvice注解实现全局异常处理
 * 使用@Slf4j注解实现日志记录功能
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * @param e 业务异常对象
     * @return 返回统一的失败结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.failed(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * @param e 参数校验异常对象
     * @return 返回统一的失败结果，包含校验失败信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", message);
        return Result.failed(message);
    }

    /**
     * 处理权限不足异常
     * @param e 权限不足异常对象
     * @return 返回统一的禁止访问结果，设置HTTP状态码为403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.forbidden(e.getMessage());
    }

    /**
     * 处理系统其他异常
     * @param e 系统异常对象
     * @return 返回统一的系统繁忙提示
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.failed("系统繁忙，请稍后重试");
    }
}