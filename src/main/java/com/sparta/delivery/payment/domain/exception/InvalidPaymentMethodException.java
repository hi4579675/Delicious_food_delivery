package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidPaymentMethodException extends BaseException {

    public InvalidPaymentMethodException() {
        super(PaymentErrorCode.INVALID_PAYMENT_METHOD);
    }

}
