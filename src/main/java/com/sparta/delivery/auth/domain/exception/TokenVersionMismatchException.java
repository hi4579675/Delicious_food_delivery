package com.sparta.delivery.auth.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class TokenVersionMismatchException extends BaseException {
    public TokenVersionMismatchException() {
        super(AuthErrorCode.TOKEN_VERSION_MISMATCH);
    }
}