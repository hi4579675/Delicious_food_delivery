package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreNotFoundException extends BaseException {

    public StoreNotFoundException() {
        super(StoreErrorCode.STORE_NOT_FOUND);
    }
}
