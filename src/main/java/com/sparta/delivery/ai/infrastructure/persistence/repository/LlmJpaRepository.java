package com.sparta.delivery.ai.infrastructure.persistence.repository;

import com.sparta.delivery.ai.domain.entity.Llm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LlmJpaRepository extends JpaRepository<Llm, UUID> {

    Optional<Llm> findByLlmIdAndDeletedAtIsNull(UUID llmId);

    Optional<Llm> findByIsActiveTrueAndDeletedAtIsNull();

    boolean existsByLlmNameAndDeletedAtIsNull(String llmName);
}
