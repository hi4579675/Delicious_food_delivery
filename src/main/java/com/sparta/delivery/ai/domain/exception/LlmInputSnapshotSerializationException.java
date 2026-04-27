package com.sparta.delivery.ai.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class LlmInputSnapshotSerializationException extends BaseException {
    public LlmInputSnapshotSerializationException() {
        super(AiErrorCode.LLM_INPUT_SNAPSHOT_SERIALIZATION_FAILED);
    }
}