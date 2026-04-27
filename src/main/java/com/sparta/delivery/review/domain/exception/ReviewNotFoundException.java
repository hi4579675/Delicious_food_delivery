package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ReviewNotFoundException extends BaseException {

    public ReviewNotFoundException() { super(ReviewErrorCode.REVIEW_NOT_FOUND); }

}
