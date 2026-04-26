package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidInputSnapshotException extends BaseException {
    public InvalidInputSnapshotException() {
        super(AiErrorCode.INVALID_INPUT_SNAPSHOT);
    }
}
