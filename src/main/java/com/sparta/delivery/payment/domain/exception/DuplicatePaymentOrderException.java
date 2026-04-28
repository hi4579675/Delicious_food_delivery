package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicatePaymentOrderException extends BaseException {

    public DuplicatePaymentOrderException() {
        super(PaymentErrorCode.DUPLICATE_PAYMENT_ORDER);
    }

}
