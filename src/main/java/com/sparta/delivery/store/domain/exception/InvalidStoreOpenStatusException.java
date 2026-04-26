package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreOpenStatusException extends BaseException {

    public InvalidStoreOpenStatusException() {
        super(StoreErrorCode.INVALID_STORE_OPEN_STATUS);
    }
}
