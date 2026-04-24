package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderTotalPriceException extends BaseException {

    public InvalidOrderTotalPriceException() {
        super(OrderErrorCode.INVALID_ORDER_TOTAL_PRICE);
    }
}
