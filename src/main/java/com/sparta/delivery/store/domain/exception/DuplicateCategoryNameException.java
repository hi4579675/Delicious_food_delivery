package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateCategoryNameException extends BaseException {

    public DuplicateCategoryNameException() {
        super(StoreErrorCode.DUPLICATE_CATEGORY_NAME);
    }
}
