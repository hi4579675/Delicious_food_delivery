package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidMinOrderAmountException extends BaseException {

    public InvalidMinOrderAmountException() {
        super(StoreErrorCode.INVALID_MIN_ORDER_AMOUNT);
    }
}
