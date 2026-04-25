package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreNameException extends BaseException {

    public InvalidStoreNameException() {
        super(StoreErrorCode.INVALID_STORE_NAME);
    }
}
