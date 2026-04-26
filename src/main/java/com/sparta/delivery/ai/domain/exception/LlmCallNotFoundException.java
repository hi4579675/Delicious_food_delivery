package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class LlmCallNotFoundException extends BaseException {
    public LlmCallNotFoundException() {
        super(AiErrorCode.LLM_CALL_NOT_FOUND);
    }
}
