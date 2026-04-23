package com.sparta.delivery.address.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class AddressNotFoundException extends BaseException {

    public AddressNotFoundException() {
        super(AddressErrorCode.ADDRESS_NOT_FOUND);
    }
}
