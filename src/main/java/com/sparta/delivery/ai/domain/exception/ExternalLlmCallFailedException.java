package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class ExternalLlmCallFailedException extends BaseException {
    public ExternalLlmCallFailedException() {
        super(AiErrorCode.EXTERNAL_LLM_CALL_FAILED);
    }
}
