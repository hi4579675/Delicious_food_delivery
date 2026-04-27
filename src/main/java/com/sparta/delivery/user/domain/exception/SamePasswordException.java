package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class SamePasswordException extends BaseException {
    public SamePasswordException() {
        super(UserErrorCode.SAME_AS_OLD_PASSWORD);
    }
}