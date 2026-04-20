package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidPasswordException extends BaseException {
    public InvalidPasswordException() {
        super(UserErrorCode.INVALID_PASSWORD);
    }
}