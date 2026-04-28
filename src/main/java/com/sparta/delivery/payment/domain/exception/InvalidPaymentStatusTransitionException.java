package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidPaymentStatusTransitionException extends BaseException {

    public InvalidPaymentStatusTransitionException() {
        super(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
    }

}
