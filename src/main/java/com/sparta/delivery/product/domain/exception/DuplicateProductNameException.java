package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateProductNameException extends BaseException {

    public DuplicateProductNameException() {
        super(ProductErrorCode.DUPLICATE_PRODUCT_NAME);
    }
}
