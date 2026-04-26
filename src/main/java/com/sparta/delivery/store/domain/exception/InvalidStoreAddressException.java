package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreAddressException extends BaseException {

    public InvalidStoreAddressException() {
        super(StoreErrorCode.INVALID_STORE_ADDRESS);
    }
}
