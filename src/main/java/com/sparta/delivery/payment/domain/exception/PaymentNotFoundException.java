package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException() {
        super(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

}
