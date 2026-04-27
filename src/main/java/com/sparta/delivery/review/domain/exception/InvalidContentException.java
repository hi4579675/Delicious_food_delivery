package com.sparta.delivery.review.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidContentException extends BaseException {

        public InvalidContentException() {
        super(ReviewErrorCode.INVALID_CONTENT);
    }
}