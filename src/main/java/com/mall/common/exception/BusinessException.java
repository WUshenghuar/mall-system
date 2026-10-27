package com.mall.common.exception;

/** 携带业务响应码的运行时异常，由全局异常处理器转换为统一响应。 */
/**
 * BusinessException类，自定义业务异常类，继承自RuntimeException
 * 用于处理业务逻辑中出现的异常情况，提供了错误代码和错误信息的封装
 */
public class BusinessException extends RuntimeException {
    private final int code;  // 错误代码，用于标识具体的业务错误类型

    /**
     * 自定义业务异常类的构造方法
     * @param code 错误代码，用于标识具体的业务错误类型
     * @param message 错误信息，用于描述具体的错误内容
     */
    public BusinessException(int code, String message) {
        super(message);  // 调用父类（Exception）的构造方法，设置错误信息
        this.code = code;  // 设置自定义的错误代码
    }

    /**
     * 带有默认错误代码的业务异常构造方法
     * @param message 错误信息，用于描述具体的错误内容
     */
    public BusinessException(String message) {
        this(500, message);  // 调用另一个构造方法，设置默认错误代码为500
    }

    /**
     * 使用ResultCode枚举创建业务异常
     * @param resultCode 包含错误代码和错误信息的枚举
     */
    public BusinessException(com.mall.common.result.ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());  // 使用枚举中的代码和信息创建异常
    }

    /**
     * 获取错误代码
     * @return 错误代码
     */
    public int getCode() {
        return code;
    }
}
