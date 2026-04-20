package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.LlmCall;

import java.util.Optional;
import java.util.UUID;

public interface LlmCallRepository {

    LlmCall save(LlmCall llmCall);

    Optional<LlmCall> findById(UUID callId);
}
