package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException() {
        super(UserErrorCode.DUPLICATE_EMAIL);
    }
}