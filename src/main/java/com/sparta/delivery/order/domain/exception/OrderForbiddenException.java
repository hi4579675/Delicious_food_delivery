package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class OrderForbiddenException extends BaseException {

    public OrderForbiddenException() {
        super(OrderErrorCode.ORDER_FORBIDDEN);
    }
}
