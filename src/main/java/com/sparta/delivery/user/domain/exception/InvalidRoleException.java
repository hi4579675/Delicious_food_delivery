package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRoleException extends BaseException {
    public InvalidRoleException() {
        super(UserErrorCode.INVALID_ROLE);
    }
}
