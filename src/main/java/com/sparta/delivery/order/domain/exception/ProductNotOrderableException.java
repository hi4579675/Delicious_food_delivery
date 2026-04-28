package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ProductNotOrderableException extends BaseException {

    public ProductNotOrderableException() {
        super(OrderErrorCode.PRODUCT_NOT_ORDERABLE);
    }
}
