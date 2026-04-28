package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderStatusForPaymentException extends BaseException {

    public InvalidOrderStatusForPaymentException() {
        super(PaymentErrorCode.INVALID_ORDER_STATUS_FOR_PAYMENT);
    }
}

