package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;

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
            UUID productId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "아메리카노 설명 생성";
            LlmGenerateResponse response = new LlmGenerateResponse(
                    "깔끔하고 산뜻한 아메리카노 설명",
                    "{\"result\":\"ok\"}",
                    "200"
            );

            given(llmOrchestrator.generate(productId, actorId, prompt)).willReturn(response);

            // when
            String generatedDescription = aiDescriptionService.generateDescription(productId, actorId, prompt);

            // then
            assertThat(generatedDescription).isEqualTo("깔끔하고 산뜻한 아메리카노 설명");
            then(llmOrchestrator).should().generate(productId, actorId, prompt);
        }
    }
}
