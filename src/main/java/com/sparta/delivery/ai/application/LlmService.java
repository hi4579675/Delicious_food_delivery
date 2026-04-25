package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.exception.CannotDeleteActiveLlmException;
import com.sparta.delivery.ai.domain.exception.DuplicateLlmNameException;
import com.sparta.delivery.ai.domain.exception.LlmForbiddenException;
import com.sparta.delivery.ai.domain.exception.LlmNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.presentation.dto.request.LlmCreateRequest;
import com.sparta.delivery.ai.presentation.dto.request.LlmUpdateRequest;
import com.sparta.delivery.ai.presentation.dto.response.LlmResponse;
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
public class LlmService {

    private final LlmRepository llmRepository;

    @Transactional
    public LlmResponse create(Long actorId, UserRole actorRole, LlmCreateRequest request) {
        validateLlmPermission(actorRole);
        validateDuplicateLlmName(request.llmName());
        Llm llm = Llm.create(
                request.llmName(),
                request.provider(),
                false
        );
        Llm savedLlm = llmRepository.save(llm);

        log.info("LLM 생성 완료 - actorId={}, llmId={}, provider={}, llmName={}",
                actorId, savedLlm.getLlmId(), savedLlm.getProvider(), savedLlm.getLlmName());

        return LlmResponse.from(savedLlm);
    }

    public LlmResponse getLlm(UserRole actorRole, UUID llmId) {
        validateLlmPermission(actorRole);
        Llm llm = getLlmOrThrow(llmId);

        return LlmResponse.from(llm);
    }

    public List<LlmResponse> getLlms(UserRole actorRole) {
        validateLlmPermission(actorRole);
        return llmRepository.findAll(Sort.by(
                Sort.Order.desc("isActive"),
                Sort.Order.desc("updatedAt")
        )).stream()
                .map(LlmResponse::from)
                .toList();
    }

    @Transactional
    public LlmResponse changeLlmName(Long actorId, UserRole actorRole, UUID llmId, LlmUpdateRequest request) {
        validateLlmPermission(actorRole);
        Llm llm = getLlmOrThrow(llmId);
        validateDuplicateLlmNameOnUpdate(llm, request.llmName());
        llm.updateName(request.llmName());
        log.info("LLM 이름 변경 완료 - actorId={}, llmId={}, provider={}, llmName={}",
                actorId, llm.getLlmId(), llm.getProvider(), llm.getLlmName());

        return LlmResponse.from(llm);
    }

    @Transactional
    public LlmResponse delete(Long actorId, UserRole actorRole, UUID llmId) {
        validateLlmPermission(actorRole);
        Llm llm = getLlmOrThrow(llmId);
        if (llm.isActive()) {
            throw new CannotDeleteActiveLlmException();
        }

        llm.softDelete(actorId);
        log.info("LLM 삭제 완료 - actorId={}, llmId={}, provider={}, llmName={}",
                actorId, llm.getLlmId(), llm.getProvider(), llm.getLlmName());

        return LlmResponse.from(llm);
    }

    private Llm getLlmOrThrow(UUID llmId) {
        return llmRepository.findByLlmId(llmId)
                .orElseThrow(LlmNotFoundException::new);
    }

    private void validateLlmPermission(UserRole actorRole) {
        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            return;
        }
        throw new LlmForbiddenException();
    }

    private void validateDuplicateLlmName(String llmName) {
        if (llmRepository.existsIncludingDeletedByLlmName(llmName)) {
            throw new DuplicateLlmNameException();
        }
    }

    private void validateDuplicateLlmNameOnUpdate(Llm llm, String llmName) {
        boolean nameChanged = !llm.getLlmName().equals(llmName);
        if (nameChanged && llmRepository.existsIncludingDeletedByLlmName(llmName)) {
            throw new DuplicateLlmNameException();
        }
    }
}
