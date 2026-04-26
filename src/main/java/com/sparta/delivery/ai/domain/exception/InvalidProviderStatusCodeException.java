package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidProviderStatusCodeException extends BaseException {
    public InvalidProviderStatusCodeException() {
        super(AiErrorCode.INVALID_PROVIDER_STATUS_CODE);
    }
}
