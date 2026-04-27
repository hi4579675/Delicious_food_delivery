package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ActiveLlmNotFoundException extends BaseException {
    public ActiveLlmNotFoundException() {
        super(AiErrorCode.ACTIVE_LLM_NOT_FOUND);
    }
}
