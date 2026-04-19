package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidProductDescriptionException extends BaseException {

    public InvalidProductDescriptionException() {
        super(ProductErrorCode.INVALID_PRODUCT_DESCRIPTION);
    }
}
