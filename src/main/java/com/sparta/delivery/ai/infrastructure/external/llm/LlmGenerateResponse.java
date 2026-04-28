package com.sparta.delivery.ai.infrastructure.external.llm;

public record LlmGenerateResponse(
        String generatedText,
        String rawResponse,
        String finishReason
) {
}
