package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.LlmCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LlmCallRepository extends JpaRepository<LlmCall, UUID> {

    Optional<LlmCall> findByCallId(UUID callId);
}