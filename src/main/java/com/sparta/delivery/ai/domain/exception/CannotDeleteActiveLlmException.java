package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class CannotDeleteActiveLlmException extends BaseException {
    public CannotDeleteActiveLlmException() {
        super(AiErrorCode.CANNOT_DELETE_ACTIVE_LLM);
    }
}
