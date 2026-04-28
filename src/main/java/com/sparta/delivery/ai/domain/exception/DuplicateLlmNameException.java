package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateLlmNameException extends BaseException {
    public DuplicateLlmNameException() {
        super(AiErrorCode.DUPLICATE_LLM_NAME);
    }
}
