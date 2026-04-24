package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class CancelTimeExceededException extends BaseException {

    public CancelTimeExceededException() {
        super(OrderErrorCode.CANCEL_TIME_EXCEEDED);
    }
}
