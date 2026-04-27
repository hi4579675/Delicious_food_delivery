package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderStatusException extends BaseException {

    public InvalidOrderStatusException() { super(ReviewErrorCode.INVALID_ORDER_STATUS); }

}
