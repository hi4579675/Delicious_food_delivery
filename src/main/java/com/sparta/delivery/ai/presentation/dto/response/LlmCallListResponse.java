package com.sparta.delivery.ai.presentation.dto.response;

import com.sparta.delivery.ai.domain.entity.LlmCall;

import java.time.LocalDateTime;
import java.util.UUID;

public record LlmCallListResponse(
        UUID callId,
        UUID productId,
        UUID llmId,
        String finishReason,
        LocalDateTime createdAt
) {
    public static LlmCallListResponse from(LlmCall llmCall) {
        return new LlmCallListResponse(
                llmCall.getCallId(),
                llmCall.getProductId(),
                llmCall.getLlmId(),
                llmCall.getFinishReason(),
                llmCall.getCreatedAt()
        );
    }
}
