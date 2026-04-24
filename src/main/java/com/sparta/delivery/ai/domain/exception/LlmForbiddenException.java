package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class LlmForbiddenException extends BaseException {
    public LlmForbiddenException() {
        super(AiErrorCode.LLM_FORBIDDEN);
    }
}
