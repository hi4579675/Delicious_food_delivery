package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateReviewOrderException extends BaseException {

    public DuplicateReviewOrderException() { super(ReviewErrorCode.DUPLICATE_REVIEW_ORDER); }

}
