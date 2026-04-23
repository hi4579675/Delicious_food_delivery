package com.sparta.delivery.auth.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException() {
        super(AuthErrorCode.INVALID_CREDENTIALS);
    }
}