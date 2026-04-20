package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.LlmCall;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCallRepository extends JpaRepository<LlmCall, UUID> {

    Optional<LlmCall> findByCallId(UUID callId);
}