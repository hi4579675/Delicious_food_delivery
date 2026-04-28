package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class PaymentForbiddenException extends BaseException {

    public PaymentForbiddenException() {
        super(PaymentErrorCode.PAYMENT_FORBIDDEN);
    }

}
