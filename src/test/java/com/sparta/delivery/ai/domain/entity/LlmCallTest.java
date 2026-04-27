package com.sparta.delivery.ai.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sparta.delivery.ai.domain.exception.AiErrorCode;
import com.sparta.delivery.ai.domain.exception.InvalidCreatedByException;
import com.sparta.delivery.ai.domain.exception.InvalidInputSnapshotException;
import com.sparta.delivery.ai.domain.exception.InvalidProviderStatusCodeException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmCallTest {

    @Test
    @DisplayName("create should create llmCall successfully")
    void create_shouldCreateLlmCallSuccessfully() {
        // given
        UUID llmId = UUID.randomUUID();
        String inputSnapshot = "{\"productName\":\"Americano\",\"price\":4500}";
        String providerStatusCode = "200";
        String rawResponse = "{\"result\":\"ok\"}";
        String generatedText = "generated text";
        Long createdBy = 1L;

        LocalDateTime before = LocalDateTime.now();

        // when
        LlmCall llmCall = LlmCall.create(
                llmId,
                inputSnapshot,
                providerStatusCode,
                rawResponse,
                generatedText,
                createdBy
        );

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(llmCall.getLlmId()).isEqualTo(llmId);
        assertThat(llmCall.getProductId()).isNull();
        assertThat(llmCall.getInputSnapshot()).isEqualTo(inputSnapshot);
        assertThat(llmCall.getProviderStatusCode()).isEqualTo(providerStatusCode);
        assertThat(llmCall.getRawResponse()).isEqualTo(rawResponse);
        assertThat(llmCall.getGeneratedText()).isEqualTo(generatedText);
        assertThat(llmCall.getCreatedBy()).isEqualTo(createdBy);
        assertThat(llmCall.getCreatedAt()).isNotNull();
        assertThat(llmCall.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("create should throw when inputSnapshot is blank")
    void create_shouldThrow_whenInputSnapshotIsBlank() {
        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                UUID.randomUUID(),
                "   ",
                "200",
                "{}",
                "generated",
                1L
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidInputSnapshotException.class);
        InvalidInputSnapshotException exception = (InvalidInputSnapshotException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_INPUT_SNAPSHOT.getCode());
    }

    @Test
    @DisplayName("create should throw when providerStatusCode length exceeds 50")
    void create_shouldThrow_whenProviderStatusCodeLengthExceeds50() {
        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                UUID.randomUUID(),
                "{\"productName\":\"Americano\"}",
                "1".repeat(51),
                "{}",
                "generated",
                1L
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProviderStatusCodeException.class);
        InvalidProviderStatusCodeException exception = (InvalidProviderStatusCodeException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_PROVIDER_STATUS_CODE.getCode());
    }

    @Test
    @DisplayName("create should throw when createdBy is null")
    void create_shouldThrow_whenCreatedByIsNull() {
        // when
        Throwable thrown = catchThrowable(() -> LlmCall.create(
                UUID.randomUUID(),
                "{\"productName\":\"Americano\"}",
                "200",
                "{}",
                "generated",
                null
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidCreatedByException.class);
        InvalidCreatedByException exception = (InvalidCreatedByException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_CREATED_BY.getCode());
    }
}
