package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.exception.ActiveLlmNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClientRegistry;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateRequest;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmOrchestrator {

    private final LlmRepository llmRepository;
    private final LlmClientRegistry llmClientRegistry;

    public LlmGenerateResponse generate(String prompt) {
        Llm activeLlm = llmRepository.findByIsActiveTrue()
                .orElseThrow(ActiveLlmNotFoundException::new);

        LlmClient client = llmClientRegistry.getClient(activeLlm.getProvider());

        return client.generate(
                activeLlm,
                new LlmGenerateRequest(prompt)
        );
    }
}
