package com.mall.common.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;

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