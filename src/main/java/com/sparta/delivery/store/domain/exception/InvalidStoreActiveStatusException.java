package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreActiveStatusException extends BaseException {

    public InvalidStoreActiveStatusException() {
        super(StoreErrorCode.INVALID_STORE_ACTIVE_STATUS);
    }
}
