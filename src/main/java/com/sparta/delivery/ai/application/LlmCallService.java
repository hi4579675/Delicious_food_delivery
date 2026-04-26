package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.exception.AiForbiddenException;
import com.sparta.delivery.ai.domain.exception.LlmCallNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallListResponse;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmCallService {

    private final LlmCallRepository llmCallRepository;

    public LlmCallResponse getLlmCall(UserRole actorRole, UUID callId) {
        validateLlmCallPermission(actorRole);
        LlmCall call = getLlmCallOrThrow(callId);

        return LlmCallResponse.from(call);
    }

    public List<LlmCallListResponse> getLlmCalls(UserRole actorRole) {
        validateLlmCallPermission(actorRole);
        return llmCallRepository.findAll(Sort.by(
                        Sort.Order.desc("createdAt")
                ))
                .stream()
                .map(LlmCallListResponse::from)
                .toList();
    }

    private LlmCall getLlmCallOrThrow(UUID callId) {
        return llmCallRepository.findByCallId(callId)
                .orElseThrow(LlmCallNotFoundException::new);
    }

    private void validateLlmCallPermission(UserRole actorRole) {
        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            return;
        }
        throw new AiForbiddenException();
    }
}
