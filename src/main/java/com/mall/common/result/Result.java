package com.mall.common.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** API 统一响应封装，固定携带业务码、消息、数据和生成时间。 */
/**
 * 通用响应结果类，用于封装API接口返回的数据
 * 使用了Lombok的@Data和@Accessors(chain=true)注解，提供getter/setter方法和链式调用支持
 * @param <T> 泛型类型，表示返回数据的类型
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private int code;        // 响应状态码
    private String message;  // 响应消息
    private T data;         // 响应数据
    private long timestamp;  // 响应时间戳

    /**
     * 私有构造方法，防止外部直接实例化
     */
    private Result() {}

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMessage(ResultCode.SUCCESS.getMessage())
                .setData(data)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> failed(String message) {
        return new Result<T>()
                .setCode(ResultCode.FAILED.getCode())
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return new Result<T>()
                .setCode(resultCode.getCode())
                .setMessage(resultCode.getMessage())
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> failed(int code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<T>()
                .setCode(ResultCode.FORBIDDEN.getCode())
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<T>()
                .setCode(ResultCode.UNAUTHORIZED.getCode())
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }
}