package com.sparta.delivery.ai.infrastructure.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;

@ExtendWith(MockitoExtension.class)
class LlmClientRegistryTest {

    @Mock
    private LlmClient openAiClient;

    @Mock
    private LlmClient googleClient;

    @Nested
    @DisplayName("LLM client registry")
    class GetClient {

        @Test
        @DisplayName("returns client that supports provider")
        void getClient_success() {
            // given
            given(openAiClient.supports(LlmProvider.OPENAI)).willReturn(true);
            LlmClientRegistry llmClientRegistry = new LlmClientRegistry(List.of(openAiClient, googleClient));

            // when
            LlmClient client = llmClientRegistry.getClient(LlmProvider.OPENAI);

            // then
            assertThat(client).isEqualTo(openAiClient);
        }

        @Test
        @DisplayName("returns google client that supports GOOGLE provider")
        void getClient_success_google() {
            // given
            given(openAiClient.supports(LlmProvider.GOOGLE)).willReturn(false);
            given(googleClient.supports(LlmProvider.GOOGLE)).willReturn(true);
            LlmClientRegistry llmClientRegistry = new LlmClientRegistry(List.of(openAiClient, googleClient));

            // when
            LlmClient client = llmClientRegistry.getClient(LlmProvider.GOOGLE);

            // then
            assertThat(client).isEqualTo(googleClient);
        }

        @Test
        @DisplayName("throws when no client supports provider")
        void getClient_fail_whenClientNotFound() {
            // given
            given(openAiClient.supports(LlmProvider.ANTHROPIC)).willReturn(false);
            given(googleClient.supports(LlmProvider.ANTHROPIC)).willReturn(false);
            LlmClientRegistry llmClientRegistry = new LlmClientRegistry(List.of(openAiClient, googleClient));

            // when // then
            assertThatThrownBy(() -> llmClientRegistry.getClient(LlmProvider.ANTHROPIC))
                    .isInstanceOf(ExternalLlmCallFailedException.class);
        }
    }
}
