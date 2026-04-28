package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderItemQuantityException extends BaseException {

    public InvalidOrderItemQuantityException() {
        super(OrderErrorCode.INVALID_ORDER_ITEM_QUANTITY);
    }
}
