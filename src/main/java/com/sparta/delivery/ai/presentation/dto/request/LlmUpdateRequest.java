package com.sparta.delivery.ai.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LlmUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String llmName
) {
}
