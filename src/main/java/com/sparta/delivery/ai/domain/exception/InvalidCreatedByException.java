package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidCreatedByException extends BaseException {
    public InvalidCreatedByException() {
        super(AiErrorCode.INVALID_CREATED_BY);
    }
}
