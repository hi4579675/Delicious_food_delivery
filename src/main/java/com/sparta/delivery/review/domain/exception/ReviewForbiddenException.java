package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ReviewForbiddenException extends BaseException {

    public ReviewForbiddenException() { super(ReviewErrorCode.REVIEW_FORBIDDEN); }

}
