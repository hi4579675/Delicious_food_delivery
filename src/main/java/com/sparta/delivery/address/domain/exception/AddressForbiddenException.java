package com.sparta.delivery.address.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class AddressForbiddenException extends BaseException {

    public AddressForbiddenException() {
        super(AddressErrorCode.ADDRESS_FORBIDDEN);
    }
}
