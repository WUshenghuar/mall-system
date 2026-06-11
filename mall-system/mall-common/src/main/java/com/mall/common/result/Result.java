package com.mall.common.result;

import java.io.Serializable;

public class Result<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {}

    public int getCode() { return code; }
    public Result<T> setCode(int code) { this.code = code; return this; }
    public String getMessage() { return message; }
    public Result<T> setMessage(String message) { this.message = message; return this; }
    public T getData() { return data; }
    public Result<T> setData(T data) { this.data = data; return this; }
    public long getTimestamp() { return timestamp; }
    public Result<T> setTimestamp(long timestamp) { this.timestamp = timestamp; return this; }

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