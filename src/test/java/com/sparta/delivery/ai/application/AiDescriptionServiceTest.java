package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
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
                    "200"
            );

            given(llmOrchestrator.generate(eq(actorId), anyString())).willReturn(response);

            // when
            String generatedDescription = aiDescriptionService.generateDescription(
                    actorId,
                    productName,
                    price,
                    aiPromptText
            );

            // then
            assertThat(generatedDescription).isEqualTo("generated text");

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            then(llmOrchestrator).should().generate(eq(actorId), promptCaptor.capture());

            String generatedPrompt = promptCaptor.getValue();
            assertThat(generatedPrompt).contains(productName);
            assertThat(generatedPrompt).contains(String.valueOf(price));
            assertThat(generatedPrompt).contains(aiPromptText);
        }
    }
}
