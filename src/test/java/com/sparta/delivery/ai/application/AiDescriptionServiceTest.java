package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;

import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmInputSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiDescriptionServiceTest {

    @Mock
    private LlmOrchestrator llmOrchestrator;

    @InjectMocks
    private AiDescriptionService aiDescriptionService;

    @Nested
    @DisplayName("AI description")
    class GenerateDescription {

        @Test
        @DisplayName("throws ExternalLlmCallFailedException when generated text is null")
        void generateDescription_fail_whenGeneratedTextIsNull() {
            // given
            Long actorId = 1L;
            LlmGenerateResponse response = new LlmGenerateResponse(
                    null,
                    null,
                    "STOP"
            );
            given(llmOrchestrator.generate(eq(actorId), any(LlmInputSnapshot.class))).willReturn(response);

            // when & then
            assertThatThrownBy(() -> aiDescriptionService.generateDescription(
                    actorId, "Americano", 4500, null
            )).isInstanceOf(ExternalLlmCallFailedException.class);
        }

        @Test
        @DisplayName("throws ExternalLlmCallFailedException when generated text is blank")
        void generateDescription_fail_whenGeneratedTextIsBlank() {
            // given
            Long actorId = 1L;
            LlmGenerateResponse response = new LlmGenerateResponse(
                    "   ",
                    null,
                    "STOP"
            );
            given(llmOrchestrator.generate(eq(actorId), any(LlmInputSnapshot.class))).willReturn(response);

            // when & then
            assertThatThrownBy(() -> aiDescriptionService.generateDescription(
                    actorId, "Americano", 4500, null
            )).isInstanceOf(ExternalLlmCallFailedException.class);
        }

        @Test
        @DisplayName("trims generated text to 50 characters when it exceeds limit")
        void generateDescription_success_trimmedTo50() {
            // given
            Long actorId = 1L;
            String longText = "가".repeat(60); // 60자
            LlmGenerateResponse response = new LlmGenerateResponse(
                    longText,
                    null,
                    "STOP"
            );
            given(llmOrchestrator.generate(eq(actorId), any(LlmInputSnapshot.class))).willReturn(response);

            // when
            String result = aiDescriptionService.generateDescription(
                    actorId, "Americano", 4500, null
            );

            // then
            assertThat(result).hasSize(50);
        }

        @Test
        @DisplayName("returns generated text from llm orchestrator")
        void generateDescription_success() {
            // given
            Long actorId = 1L;
            String productName = "Americano";
            Integer price = 4500;
            String aiPromptText = "고소한 맛을 강조해줘";
            LlmGenerateResponse response = new LlmGenerateResponse(
                    "generated text",
                    "{\"result\":\"ok\"}",
                    "STOP"
            );

            given(llmOrchestrator.generate(eq(actorId), any(LlmInputSnapshot.class))).willReturn(response);

            // when
            String generatedDescription = aiDescriptionService.generateDescription(
                    actorId,
                    productName,
                    price,
                    aiPromptText
            );

            // then
            assertThat(generatedDescription).isEqualTo("generated text");

            ArgumentCaptor<LlmInputSnapshot> inputSnapshotCaptor = ArgumentCaptor.forClass(LlmInputSnapshot.class);
            then(llmOrchestrator).should().generate(eq(actorId), inputSnapshotCaptor.capture());

            LlmInputSnapshot capturedSnapshot = inputSnapshotCaptor.getValue();
            assertThat(capturedSnapshot.productName()).isEqualTo(productName);
            assertThat(capturedSnapshot.price()).isEqualTo(price);
            assertThat(capturedSnapshot.aiPromptText()).isEqualTo(aiPromptText);
            assertThat(capturedSnapshot.prompt()).contains(productName);
            assertThat(capturedSnapshot.prompt()).contains(String.valueOf(price));
            assertThat(capturedSnapshot.prompt()).contains(aiPromptText);
        }
    }
}
