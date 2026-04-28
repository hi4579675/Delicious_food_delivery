package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmInputSnapshot;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AiDescriptionService {

    private final LlmOrchestrator llmOrchestrator;

    @Async("llmAsyncExecutor")
    public CompletableFuture<String> generateDescription(
            Long actorId,
            String productName,
            Integer price,
            String aiPromptText
    ) {
        String prompt = buildPrompt(productName, price, aiPromptText);

        LlmInputSnapshot inputSnapshot = new LlmInputSnapshot(
                prompt,
                productName,
                price,
                aiPromptText
        );

        String generatedText = llmOrchestrator.generate(actorId, inputSnapshot).generatedText();

        if (generatedText == null || generatedText.isBlank()) {
            throw new ExternalLlmCallFailedException();
        }

        String trimmed = generatedText.length() > 50
                ? generatedText.substring(0, 50)
                : generatedText;

        return CompletableFuture.completedFuture(trimmed);
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
