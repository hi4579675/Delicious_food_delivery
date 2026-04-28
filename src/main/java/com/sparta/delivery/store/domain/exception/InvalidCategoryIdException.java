package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCategoryIdException extends BaseException {

    public InvalidCategoryIdException() {
        super(StoreErrorCode.INVALID_CATEGORY_ID);
    }
}
