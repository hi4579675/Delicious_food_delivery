package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreForbiddenException extends BaseException {

    public StoreForbiddenException() {
        super(StoreErrorCode.STORE_FORBIDDEN);
    }
}
