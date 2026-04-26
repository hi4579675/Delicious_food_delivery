package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateCategorySortOrderException extends BaseException {

    public DuplicateCategorySortOrderException() {
        super(StoreErrorCode.DUPLICATE_CATEGORY_SORT_ORDER);
    }
}
