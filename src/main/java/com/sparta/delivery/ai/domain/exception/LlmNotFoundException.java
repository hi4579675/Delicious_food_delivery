package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class LlmNotFoundException extends BaseException {
    public LlmNotFoundException() {
        super(AiErrorCode.LLM_NOT_FOUND);
    }
}
