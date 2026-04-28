package com.sparta.delivery.ai.domain.vo;

import com.sparta.delivery.ai.domain.entity.LlmProvider;

import java.util.UUID;

public record ActiveLlmInfo(
        UUID llmId,
        String llmName,
        LlmProvider provider
) {}