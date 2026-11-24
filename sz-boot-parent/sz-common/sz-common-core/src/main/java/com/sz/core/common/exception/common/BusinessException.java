package com.sz.core.common.exception.common;

import java.io.Serial;

public class BusinessException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(IResponseEnum responseEnum, Object[] args, String message) {
        super(responseEnum, args, message);
    }

    public BusinessException(IResponseEnum responseEnum, Object[] args, String message, Throwable cause) {
        super(responseEnum, args, message, cause);
    }

    public BusinessException(IResponseEnum responseEnum) {
        super(responseEnum, new Object[0], responseEnum.getMessage());
    }

    public BusinessException(IResponseEnum responseEnum, String message) {
        super(responseEnum, new Object[0], message);
    }
}
