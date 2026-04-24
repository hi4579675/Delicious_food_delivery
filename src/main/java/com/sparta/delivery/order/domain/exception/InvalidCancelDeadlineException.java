package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCancelDeadlineException extends BaseException {

    public InvalidCancelDeadlineException() {
        super(OrderErrorCode.INVALID_CANCEL_DEADLINE);
    }
}
