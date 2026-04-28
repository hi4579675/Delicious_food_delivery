package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InactiveStoreCategoryException extends BaseException {

    public InactiveStoreCategoryException() {
        super(StoreErrorCode.INACTIVE_STORE_CATEGORY);
    }
}
