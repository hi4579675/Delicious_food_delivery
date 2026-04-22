package com.sparta.delivery.ai.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LlmCallTest {

    @Test
    @DisplayName("create should create llmCall successfully")
    void create_shouldCreateLlmCallSuccessfully() {
        // given
        UUID llmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String inputSnapshot = "{\"productName\":\"Americano\",\"price\":4500}";
        String providerStatusCode = "200";
        String rawResponse = "{\"result\":\"ok\"}";
        String generatedText = "깔끔한 커피 설명";
        Long createdBy = 1L;

        LocalDateTime before = LocalDateTime.now();

        // when
        LlmCall llmCall = LlmCall.create(
                llmId,
                productId,
                inputSnapshot,
                providerStatusCode,
                rawResponse,
                generatedText,
                createdBy
        );

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(llmCall.getLlmId()).isEqualTo(llmId);
        assertThat(llmCall.getProductId()).isEqualTo(productId);
        assertThat(llmCall.getInputSnapshot()).isEqualTo(inputSnapshot);
        assertThat(llmCall.getProviderStatusCode()).isEqualTo(providerStatusCode);
        assertThat(llmCall.getRawResponse()).isEqualTo(rawResponse);
        assertThat(llmCall.getGeneratedText()).isEqualTo(generatedText);
        assertThat(llmCall.getCreatedBy()).isEqualTo(createdBy);
        assertThat(llmCall.getCreatedAt()).isNotNull();
        assertThat(llmCall.getCreatedAt()).isBetween(before, after);
    }
}
