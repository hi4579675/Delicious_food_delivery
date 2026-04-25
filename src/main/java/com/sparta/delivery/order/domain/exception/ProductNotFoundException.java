package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ProductNotFoundException extends BaseException {

    public ProductNotFoundException() {
        super(OrderErrorCode.PRODUCT_NOT_FOUND);
    }
}
