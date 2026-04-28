package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidLlmNameException extends BaseException {
    public InvalidLlmNameException() {
        super(AiErrorCode.INVALID_LLM_NAME);
    }
}
