package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderException extends BaseException {

    public InvalidOrderException() {
        super(OrderErrorCode.INVALID_ORDER);
    }
}
