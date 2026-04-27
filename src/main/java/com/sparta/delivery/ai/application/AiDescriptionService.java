package com.sparta.delivery.ai.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiDescriptionService {

    private final LlmOrchestrator llmOrchestrator;

    public String generateDescription(UUID productId, Long actorId, String prompt) {
        return llmOrchestrator.generate(productId, actorId, prompt)
                .generatedText();
    }
}
