package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidProductPriceException extends BaseException {

    public InvalidProductPriceException() {
        super(ProductErrorCode.INVALID_PRODUCT_PRICE);
    }
}
