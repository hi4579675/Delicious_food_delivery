package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.exception.ActiveLlmNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClientRegistry;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateRequest;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmOrchestrator {

    private final LlmRepository llmRepository;
    private final LlmCallRepository llmCallRepository;
    private final LlmClientRegistry llmClientRegistry;

    @Transactional
    public LlmGenerateResponse generate(UUID productId, Long actorId, String prompt) {
        Llm activeLlm = llmRepository.findByIsActiveTrue()
                .orElseThrow(ActiveLlmNotFoundException::new);

        LlmClient client = llmClientRegistry.getClient(activeLlm.getProvider());

        LlmGenerateResponse response = client.generate(
                activeLlm,
                new LlmGenerateRequest(prompt)
        );

        LlmCall llmCall = LlmCall.create(
                activeLlm.getLlmId(),
                productId,
                // TODO: Product 연계 시 inputSnapshot을 구조화된 요청 값으로 확장
                prompt,
                response.providerStatusCode(),
                response.rawResponse(),
                response.generatedText(),
                actorId
        );

        llmCallRepository.save(llmCall);

        return response;
    }
}
