package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidReviewCountException extends BaseException {

    public InvalidReviewCountException() {
        super(StoreErrorCode.INVALID_REVIEW_COUNT);
    }
}
