package com.mall.common.exception;

/** 携带业务响应码的运行时异常，由全局异常处理器转换为统一响应。 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(com.mall.common.result.ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public int getCode() {
        return code;
    }
}
