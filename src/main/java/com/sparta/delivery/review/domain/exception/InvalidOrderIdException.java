package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidOrderIdException extends BaseException {

    public InvalidOrderIdException() { super(ReviewErrorCode.INVALID_ORDER_ID); }

}
