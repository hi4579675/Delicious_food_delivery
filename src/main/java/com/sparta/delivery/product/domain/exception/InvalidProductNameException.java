package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidProductNameException extends BaseException {

    public InvalidProductNameException() {
        super(ProductErrorCode.INVALID_PRODUCT_NAME);
    }
}
