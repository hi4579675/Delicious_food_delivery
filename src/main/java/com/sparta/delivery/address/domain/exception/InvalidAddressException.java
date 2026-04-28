package com.sparta.delivery.address.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidAddressException extends BaseException {

    public InvalidAddressException() {
        super(AddressErrorCode.INVALID_ADDRESS);
    }
}
