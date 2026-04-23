package com.sparta.delivery.auth.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class TokenExpiredException extends BaseException {
    public TokenExpiredException() {
        super(AuthErrorCode.TOKEN_EXPIRED);
    }
}