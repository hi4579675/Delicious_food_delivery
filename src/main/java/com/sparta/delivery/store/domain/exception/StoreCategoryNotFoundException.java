package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreCategoryNotFoundException extends BaseException {

    public StoreCategoryNotFoundException() {
        super(StoreErrorCode.STORE_CATEGORY_NOT_FOUND);
    }
}
