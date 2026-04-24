package com.sparta.delivery.product.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ProductForbiddenException extends BaseException {

    public ProductForbiddenException() {
        super(ProductErrorCode.PRODUCT_FORBIDDEN);
    }
}
