package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidLlmProviderException extends BaseException {
    public InvalidLlmProviderException() {
        super(AiErrorCode.INVALID_LLM_PROVIDER);
    }
}
