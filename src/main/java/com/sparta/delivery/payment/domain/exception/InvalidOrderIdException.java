package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderIdException extends BaseException {

    public InvalidOrderIdException() {
        super(PaymentErrorCode.INVALID_ORDER_ID);
    }

}
