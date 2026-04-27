package com.sparta.delivery.ai.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiDescriptionService {

    private final LlmOrchestrator llmOrchestrator;

    public String generateDescription(
            UUID productId,
            Long actorId,
            String productName,
            Integer price,
            String aiPromptText
    ) {
        String prompt = buildPrompt(productName, price, aiPromptText);

        return llmOrchestrator.generate(productId, actorId, prompt)
                .generatedText();
    }

    private String buildPrompt(String productName, Integer price, String aiPromptText) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("상품명: ").append(productName).append("\n");
        promptBuilder.append("가격: ").append(price).append("원\n");
        promptBuilder.append("위 정보를 바탕으로 음식 상품 설명을 자연스럽게 작성해주세요.");

        if (aiPromptText != null && !aiPromptText.isBlank()) {
            promptBuilder.append("\n추가 요청사항: ").append(aiPromptText.trim());
        }

        return promptBuilder.toString();
    }
}
