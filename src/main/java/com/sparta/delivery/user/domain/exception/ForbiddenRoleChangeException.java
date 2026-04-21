package com.sparta.delivery.user.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ForbiddenRoleChangeException extends BaseException {
    public ForbiddenRoleChangeException() {
        super(UserErrorCode.FORBIDDEN_ROLE_CHANGE);
    }
}