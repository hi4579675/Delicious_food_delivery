package com.sparta.delivery.ai.infrastructure.external.llm;

public record LlmInputSnapshot(
        String prompt,
        String productName,
        Integer price,
        String aiPromptText
) {
}
