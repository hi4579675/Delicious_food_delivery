package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCategoryActiveStatusException extends BaseException {

    public InvalidCategoryActiveStatusException() {
        super(StoreErrorCode.INVALID_CATEGORY_ACTIVE_STATUS);
    }
}
