package com.sparta.delivery.address.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidUserIdException extends BaseException {

    public InvalidUserIdException() {
        super(AddressErrorCode.INVALID_USER_ID);
    }
}
