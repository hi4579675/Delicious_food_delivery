package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreIdException extends BaseException {

    public InvalidStoreIdException() {
        super(ProductErrorCode.INVALID_STORE_ID);
    }
}
