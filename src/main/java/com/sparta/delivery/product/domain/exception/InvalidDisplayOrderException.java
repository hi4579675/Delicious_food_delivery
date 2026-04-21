package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidDisplayOrderException extends BaseException {

    public InvalidDisplayOrderException() {
        super(ProductErrorCode.INVALID_DISPLAY_ORDER);
    }
}
