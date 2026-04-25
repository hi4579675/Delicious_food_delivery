package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRatingException extends BaseException {

        public InvalidRatingException() { super(ReviewErrorCode.INVALID_RATING); }
}
