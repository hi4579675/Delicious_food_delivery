package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class AiForbiddenException extends BaseException {
    public AiForbiddenException() {
        super(AiErrorCode.AI_FORBIDDEN);
    }
}
