package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderItemUnitPriceException extends BaseException {

    public InvalidOrderItemUnitPriceException() {
        super(OrderErrorCode.INVALID_ORDER_ITEM_UNIT_PRICE);
    }
}
