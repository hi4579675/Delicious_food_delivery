package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderStatusException extends BaseException {

    public InvalidOrderStatusException() {
        super(OrderErrorCode.INVALID_ORDER_STATUS);
    }
}
