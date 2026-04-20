package com.sparta.delivery.ai.infrastructure.persistence.repository;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LlmRepositoryImpl implements LlmRepository {

    private final LlmJpaRepository llmJpaRepository;

    @Override
    public Llm save(Llm llm) {
        return llmJpaRepository.save(llm);
    }

    @Override
    public Optional<Llm> findById(UUID llmId) {
        return llmJpaRepository.findByLlmIdAndDeletedAtIsNull(llmId);
    }

    @Override
    public Optional<Llm> findActive() {
        return llmJpaRepository.findByIsActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public boolean existsByLlmName(String llmName) {
        return llmJpaRepository.existsByLlmNameAndDeletedAtIsNull(llmName);
    }
}
