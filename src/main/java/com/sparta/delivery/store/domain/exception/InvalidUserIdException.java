package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidUserIdException extends BaseException {

    public InvalidUserIdException() {
        super(StoreErrorCode.INVALID_USER_ID);
    }
}
