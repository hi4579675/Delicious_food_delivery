package com.sparta.delivery.ai.presentation.dto.request;

import com.sparta.delivery.ai.domain.entity.LlmProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LlmCreateRequest(
        @NotBlank
        @Size(max = 100)
        String llmName,

        @NotNull
        LlmProvider provider
) {
}
