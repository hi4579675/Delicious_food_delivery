package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class MinOrderAmountNotMetException extends BaseException {

    public MinOrderAmountNotMetException() {
        super(OrderErrorCode.MIN_ORDER_AMOUNT_NOT_MET);
    }
}
