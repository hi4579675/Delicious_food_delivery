package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderItemException extends BaseException {

    public InvalidOrderItemException() {
        super(OrderErrorCode.INVALID_ORDER_ITEM);
    }
}
