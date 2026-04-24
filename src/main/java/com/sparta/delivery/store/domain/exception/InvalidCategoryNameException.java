package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCategoryNameException extends BaseException {

    public InvalidCategoryNameException() {
        super(StoreErrorCode.INVALID_CATEGORY_NAME);
    }
}
