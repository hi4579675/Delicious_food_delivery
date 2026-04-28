package com.sparta.delivery.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.exception.LlmInputSnapshotSerializationException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.domain.vo.ActiveLlmInfo;
import com.sparta.delivery.ai.infrastructure.external.llm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmOrchestrator {

    private final LlmService llmService;
    private final LlmCallRepository llmCallRepository;
    private final LlmClientRegistry llmClientRegistry;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LlmGenerateResponse generate(Long actorId, LlmInputSnapshot inputSnapshot) {
        ActiveLlmInfo activeLlm = llmService.getActiveLlm();

        String serializedInputSnapshot = createJsonInputSnapshot(inputSnapshot);

        LlmClient client = llmClientRegistry.getClient(activeLlm.provider());

        LlmGenerateResponse response = client.generate(
                activeLlm,
                new LlmGenerateRequest(inputSnapshot.prompt())
        );

        LlmCall llmCall = LlmCall.create(
                activeLlm.llmId(),
                serializedInputSnapshot,
                response.finishReason(),
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
