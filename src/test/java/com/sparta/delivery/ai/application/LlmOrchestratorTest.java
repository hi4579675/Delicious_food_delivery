package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ActiveLlmNotFoundException;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClientRegistry;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;

@ExtendWith(MockitoExtension.class)
class LlmOrchestratorTest {

    @Mock
    private LlmRepository llmRepository;

    @Mock
    private LlmCallRepository llmCallRepository;

    @Mock
    private LlmClientRegistry llmClientRegistry;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private LlmOrchestrator llmOrchestrator;

    @Nested
    @DisplayName("LLM orchestration")
    class Generate {

        @Test
        @DisplayName("generates text with active llm and saves llm call log")
        void generate_success() {
            // given
            UUID llmId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "아메리카노 설명 생성";
            Llm activeLlm = createLlm(llmId, "gpt-5.4-mini", LlmProvider.OPENAI, true);
            LlmGenerateResponse llmGenerateResponse = new LlmGenerateResponse(
                    "깔끔하고 산뜻한 아메리카노 설명",
                    "{\"result\":\"ok\"}",
                    "200"
            );

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(activeLlm));
            given(llmClientRegistry.getClient(LlmProvider.OPENAI)).willReturn(llmClient);
            given(llmClient.generate(eq(activeLlm), any())).willReturn(llmGenerateResponse);
            given(llmCallRepository.save(any(LlmCall.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            LlmGenerateResponse response = llmOrchestrator.generate(productId, actorId, prompt);

            // then
            assertThat(response).isEqualTo(llmGenerateResponse);

            ArgumentCaptor<LlmCall> llmCallCaptor = ArgumentCaptor.forClass(LlmCall.class);
            then(llmCallRepository).should().save(llmCallCaptor.capture());

            LlmCall savedCall = llmCallCaptor.getValue();
            assertThat(savedCall.getLlmId()).isEqualTo(llmId);
            assertThat(savedCall.getProductId()).isEqualTo(productId);
            assertThat(savedCall.getInputSnapshot()).isEqualTo(prompt);
            assertThat(savedCall.getProviderStatusCode()).isEqualTo("200");
            assertThat(savedCall.getRawResponse()).isEqualTo("{\"result\":\"ok\"}");
            assertThat(savedCall.getGeneratedText()).isEqualTo("깔끔하고 산뜻한 아메리카노 설명");
            assertThat(savedCall.getCreatedBy()).isEqualTo(actorId);
            assertThat(savedCall.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws when active llm does not exist")
        void generate_fail_whenActiveLlmNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "아메리카노 설명 생성";

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> llmOrchestrator.generate(productId, actorId, prompt))
                    .isInstanceOf(ActiveLlmNotFoundException.class);

            then(llmClientRegistry).shouldHaveNoInteractions();
            then(llmCallRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("does not save llm call log when external llm call fails")
        void generate_fail_whenExternalLlmCallFails() {
            // given
            UUID llmId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "아메리카노 설명 생성";
            Llm activeLlm = createLlm(llmId, "gpt-5.4-mini", LlmProvider.OPENAI, true);

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(activeLlm));
            given(llmClientRegistry.getClient(LlmProvider.OPENAI)).willReturn(llmClient);
            given(llmClient.generate(eq(activeLlm), any())).willThrow(new ExternalLlmCallFailedException());

            // when // then
            assertThatThrownBy(() -> llmOrchestrator.generate(productId, actorId, prompt))
                    .isInstanceOf(ExternalLlmCallFailedException.class);

            then(llmCallRepository).should(never()).save(any(LlmCall.class));
        }
    }

    private Llm createLlm(UUID llmId, String llmName, LlmProvider provider, boolean isActive) {
        Llm llm = Llm.create(llmName, provider, isActive);
        ReflectionTestUtils.setField(llm, "llmId", llmId);
        ReflectionTestUtils.setField(llm, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(llm, "updatedAt", LocalDateTime.now());
        return llm;
    }
}
