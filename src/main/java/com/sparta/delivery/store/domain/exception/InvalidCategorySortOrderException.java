package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCategorySortOrderException extends BaseException {

    public InvalidCategorySortOrderException() {
        super(StoreErrorCode.INVALID_CATEGORY_SORT_ORDER);
    }
}
