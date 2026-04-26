package com.sparta.delivery.ai.presentation.dto.response;

import com.sparta.delivery.ai.domain.entity.LlmCall;

import java.time.LocalDateTime;
import java.util.UUID;

public record LlmCallResponse(
        UUID callId,
        UUID productId,
        UUID llmId,
        String inputSnapshot,
        String providerStatusCode,
        String rawResponse,
        String generatedText,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static LlmCallResponse from(LlmCall llmCall) {
        return new LlmCallResponse(
                llmCall.getCallId(),
                llmCall.getProductId(),
                llmCall.getLlmId(),
                llmCall.getInputSnapshot(),
                llmCall.getProviderStatusCode(),
                llmCall.getRawResponse(),
                llmCall.getGeneratedText(),
                llmCall.getCreatedAt(),
                llmCall.getCreatedBy()
        );
    }
}
