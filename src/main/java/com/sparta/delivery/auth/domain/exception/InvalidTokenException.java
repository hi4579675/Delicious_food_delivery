package com.sparta.delivery.auth.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException() {
        super(AuthErrorCode.INVALID_TOKEN);
    }
}