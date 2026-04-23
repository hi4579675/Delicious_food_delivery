package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidTotalPriceException extends BaseException {

    public InvalidTotalPriceException() {
        super(PaymentErrorCode.INVALID_TOTAL_PRICE);
    }

}
