package com.sylvie233.common.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public static BizException of(String msg) {
        return new BizException(msg);
    }

    public static BizException of(int code, String msg) {
        return new BizException(code, msg);
    }
}
