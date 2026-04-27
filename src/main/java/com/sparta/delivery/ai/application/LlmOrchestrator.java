package com.sparta.delivery.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.exception.ActiveLlmNotFoundException;
import com.sparta.delivery.ai.domain.exception.LlmInputSnapshotSerializationException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.infrastructure.external.llm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmOrchestrator {

    private final LlmRepository llmRepository;
    private final LlmCallRepository llmCallRepository;
    private final LlmClientRegistry llmClientRegistry;
    private final ObjectMapper objectMapper;

    @Transactional
    public LlmGenerateResponse generate(Long actorId, LlmInputSnapshot inputSnapshot) {
        Llm activeLlm = llmRepository.findByIsActiveTrue()
                .orElseThrow(ActiveLlmNotFoundException::new);

        String serializedInputSnapshot = createJsonInputSnapshot(inputSnapshot);

        LlmClient client = llmClientRegistry.getClient(activeLlm.getProvider());

        LlmGenerateResponse response = client.generate(
                activeLlm,
                new LlmGenerateRequest(inputSnapshot.prompt())
        );

        LlmCall llmCall = LlmCall.create(
                activeLlm.getLlmId(),
                serializedInputSnapshot,
                response.providerStatusCode(),
                response.rawResponse(),
                response.generatedText(),
                actorId
        );

        llmCallRepository.save(llmCall);

        return response;
    }

    private String createJsonInputSnapshot(LlmInputSnapshot inputSnapshot) {
        try {
            return objectMapper.writeValueAsString(inputSnapshot);
        } catch (JsonProcessingException e) {
            log.error("LLM inputSnapshot 직렬화 실패 - productId용 snapshot 생성 중", e);
            throw new LlmInputSnapshotSerializationException();
        }
    }
}
