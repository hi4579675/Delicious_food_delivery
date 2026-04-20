package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.Llm;

import java.util.Optional;
import java.util.UUID;

public interface LlmRepository {

    Llm save(Llm llm);

    Optional<Llm> findById(UUID llmId);

    Optional<Llm> findActive();

    boolean existsByLlmName(String llmName);
}
