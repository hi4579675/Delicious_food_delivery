package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.exception.*;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.presentation.dto.request.LlmCreateRequest;
import com.sparta.delivery.ai.presentation.dto.request.LlmUpdateRequest;
import com.sparta.delivery.ai.presentation.dto.response.LlmResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Page<LlmResponse> getLlms(UserRole actorRole, int page, int size, String sort, String keyword) {
        validateLlmPermission(actorRole);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                normalizeSort(sort)
        );
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            return llmRepository.findByLlmNameContainingIgnoreCase(normalizedKeyword, pageable)
                    .map(LlmResponse::from);
        }
        return llmRepository.findAll(pageable)
                .map(LlmResponse::from);
    }

    private int normalizePageSize(int size) {
        return (size == 10 || size == 30 || size == 50) ? size : 10;
    }

    private Sort normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("isActive"), Sort.Order.desc("updatedAt"));
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";

        if (!property.equals("isActive") && !property.equals("updatedAt") && !property.equals("llmName")) {
            return Sort.by(Sort.Order.desc("isActive"), Sort.Order.desc("updatedAt"));
        }
        Sort.Direction sortDirection = direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, property);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return keyword.trim();
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
    @CacheEvict(cacheNames = "activeLlm", key = "'current'")
    public LlmResponse activate(Long actorId, UserRole actorRole, UUID llmId) {
        validateLlmPermission(actorRole);
        Llm target = getLlmOrThrow(llmId);

        if (target.isActive()) {
            return LlmResponse.from(target);
        }
        deactivateCurrentActiveLlm();

        target.activate();
        log.info("LLM 활성화 완료 - actorId={}, llmId={}, provider={}, llmName={}",
                actorId, target.getLlmId(), target.getProvider(), target.getLlmName());

        return LlmResponse.from(target);
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
        throw new AiForbiddenException();
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

    private void deactivateCurrentActiveLlm() {
        llmRepository.findByIsActiveTrueForUpdate()
                .ifPresent(Llm::deactivate);
    }

    @Cacheable(cacheNames = "activeLlm", key = "'current'")
    public Llm getActiveLlm() {
        return llmRepository.findByIsActiveTrue()
                .orElseThrow(ActiveLlmNotFoundException::new);
    }
}
