package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.Llm;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmRepository extends JpaRepository<Llm, UUID> {

    Optional<Llm> findByLlmIdAndDeletedAtIsNull(UUID llmId);

    Optional<Llm> findByIsActiveTrueAndDeletedAtIsNull();

    boolean existsByLlmNameAndDeletedAtIsNull(String llmName);
}