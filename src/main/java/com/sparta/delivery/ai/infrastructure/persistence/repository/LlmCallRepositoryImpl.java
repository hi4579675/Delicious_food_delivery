package com.sparta.delivery.ai.infrastructure.persistence.repository;

import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LlmCallRepositoryImpl implements LlmCallRepository {

    private final LlmCallJpaRepository llmCallJpaRepository;

    @Override
    public LlmCall save(LlmCall llmCall) {
        return llmCallJpaRepository.save(llmCall);
    }

    @Override
    public Optional<LlmCall> findById(UUID callId) {
        return llmCallJpaRepository.findByCallId(callId);
    }

}
