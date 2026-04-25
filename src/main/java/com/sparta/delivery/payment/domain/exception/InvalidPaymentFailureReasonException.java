package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidPaymentFailureReasonException extends BaseException {

    public InvalidPaymentFailureReasonException() {
        super(PaymentErrorCode.INVALID_PAYMENT_FAILURE_REASON);
    }
}

