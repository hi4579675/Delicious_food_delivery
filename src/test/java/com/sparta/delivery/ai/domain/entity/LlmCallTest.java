package com.sparta.delivery.ai.domain.entity;

import com.sparta.delivery.ai.domain.exception.AiErrorCode;
import com.sparta.delivery.ai.domain.exception.InvalidCreatedByException;
import com.sparta.delivery.ai.domain.exception.InvalidInputSnapshotException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LlmCallTest {

    @Test
    @DisplayName("create should create llmCall successfully")
    void create_shouldCreateLlmCallSuccessfully() {
        // given
        UUID callId = UUID.randomUUID();
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
                callId,
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
        assertThat(llmCall.getCallId()).isEqualTo(callId);
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

    @Test
    @DisplayName("create should throw when inputSnapshot is null")
    void create_shouldThrow_whenInputSnapshotIsNull() {
        // given
        UUID callId = UUID.randomUUID();
        UUID llmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                callId,
                llmId,
                productId,
                null,
                "200",
                "{\"result\":\"ok\"}",
                "generated text",
                1L
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidInputSnapshotException.class);
        InvalidInputSnapshotException exception = (InvalidInputSnapshotException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_INPUT_SNAPSHOT.getCode());
    }

    @Test
    @DisplayName("create should throw when inputSnapshot is blank")
    void create_shouldThrow_whenInputSnapshotIsBlank() {
        // given
        UUID callId = UUID.randomUUID();
        UUID llmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                callId,
                llmId,
                productId,
                "   ",
                "200",
                "{\"result\":\"ok\"}",
                "generated text",
                1L
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidInputSnapshotException.class);
        InvalidInputSnapshotException exception = (InvalidInputSnapshotException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_INPUT_SNAPSHOT.getCode());
    }

    @Test
    @DisplayName("create should throw when createdBy is null")
    void create_shouldThrow_whenCreatedByIsNull() {
        // given
        UUID callId = UUID.randomUUID();
        UUID llmId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                callId,
                llmId,
                productId,
                "{\"productName\":\"Americano\"}",
                "200",
                "{\"result\":\"ok\"}",
                "generated text",
                null
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidCreatedByException.class);
        InvalidCreatedByException exception = (InvalidCreatedByException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_CREATED_BY.getCode());
    }
}