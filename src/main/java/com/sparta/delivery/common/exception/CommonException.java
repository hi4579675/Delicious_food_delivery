package com.sparta.delivery.common.exception;

public class CommonException extends BaseException {

    public CommonException(CommonErrorCode errorCode) {
        super(errorCode);
    }

    public CommonException(CommonErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}