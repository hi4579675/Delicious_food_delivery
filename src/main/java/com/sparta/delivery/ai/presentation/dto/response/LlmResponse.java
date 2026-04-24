package com.sparta.delivery.ai.presentation.dto.response;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import org.springframework.scheduling.quartz.ResourceLoaderClassLoadHelper;

import java.time.LocalDateTime;
import java.util.UUID;

public record LlmResponse(
        UUID llmId,
        String llmName,
        LlmProvider provider,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LlmResponse from(Llm llm) {
        return new LlmResponse(
                llm.getLlmId(),
                llm.getLlmName(),
                llm.getProvider(),
                llm.isActive(),
                llm.getCreatedAt(),
                llm.getUpdatedAt()
        );
    }
}
