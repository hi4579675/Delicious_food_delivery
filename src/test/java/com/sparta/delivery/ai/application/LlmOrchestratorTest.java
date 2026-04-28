package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ActiveLlmNotFoundException;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.domain.exception.LlmInputSnapshotSerializationException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClientRegistry;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmInputSnapshot;
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

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LlmOrchestrator llmOrchestrator;

    @Nested
    @DisplayName("LLM orchestration")
    class Generate {

        @Test
        @DisplayName("generates text with active llm and saves llm call log")
        void generate_success() throws Exception {
            // given
            UUID llmId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "prompt";
            LlmInputSnapshot llmInputSnapshot = new LlmInputSnapshot(prompt, "Americano", 4500, "고소한 맛을 강조해줘");
            String inputSnapshot = "{\"prompt\":\"prompt\"}";
            Llm activeLlm = createLlm(llmId, "gpt-5.4-mini", LlmProvider.OPENAI, true);
            LlmGenerateResponse llmGenerateResponse = new LlmGenerateResponse(
                    "generated text",
                    "{\"result\":\"ok\"}",
                    "200"
            );

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(activeLlm));
            given(objectMapper.writeValueAsString(any(LlmInputSnapshot.class))).willReturn(inputSnapshot);
            given(llmClientRegistry.getClient(LlmProvider.OPENAI)).willReturn(llmClient);
            given(llmClient.generate(eq(activeLlm), any())).willReturn(llmGenerateResponse);
            given(llmCallRepository.save(any(LlmCall.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            LlmGenerateResponse response = llmOrchestrator.generate(actorId, llmInputSnapshot);

            // then
            assertThat(response).isEqualTo(llmGenerateResponse);

            ArgumentCaptor<LlmCall> llmCallCaptor = ArgumentCaptor.forClass(LlmCall.class);
            then(llmCallRepository).should().save(llmCallCaptor.capture());

            LlmCall savedCall = llmCallCaptor.getValue();
            assertThat(savedCall.getLlmId()).isEqualTo(llmId);
            assertThat(savedCall.getProductId()).isNull();
            assertThat(savedCall.getInputSnapshot()).isEqualTo(inputSnapshot);
            assertThat(savedCall.getFinishReason()).isEqualTo("200");
            assertThat(savedCall.getRawResponse()).isEqualTo("{\"result\":\"ok\"}");
            assertThat(savedCall.getGeneratedText()).isEqualTo("generated text");
            assertThat(savedCall.getCreatedBy()).isEqualTo(actorId);
            assertThat(savedCall.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws when active llm does not exist")
        void generate_fail_whenActiveLlmNotFound() {
            // given
            Long actorId = 1L;
            LlmInputSnapshot llmInputSnapshot = new LlmInputSnapshot("prompt", "Americano", 4500, null);

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> llmOrchestrator.generate(actorId, llmInputSnapshot))
                    .isInstanceOf(ActiveLlmNotFoundException.class);

            then(llmClientRegistry).shouldHaveNoInteractions();
            then(llmCallRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("does not save llm call log when external llm call fails")
        void generate_fail_whenExternalLlmCallFails() throws Exception {
            // given
            UUID llmId = UUID.randomUUID();
            Long actorId = 1L;
            String prompt = "prompt";
            LlmInputSnapshot llmInputSnapshot = new LlmInputSnapshot(prompt, "Americano", 4500, "고소한 맛을 강조해줘");
            String inputSnapshot = "{\"prompt\":\"prompt\"}";
            Llm activeLlm = createLlm(llmId, "gpt-5.4-mini", LlmProvider.OPENAI, true);

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(activeLlm));
            given(objectMapper.writeValueAsString(any(LlmInputSnapshot.class))).willReturn(inputSnapshot);
            given(llmClientRegistry.getClient(LlmProvider.OPENAI)).willReturn(llmClient);
            given(llmClient.generate(eq(activeLlm), any())).willThrow(new ExternalLlmCallFailedException());

            // when // then
            assertThatThrownBy(() -> llmOrchestrator.generate(actorId, llmInputSnapshot))
                    .isInstanceOf(ExternalLlmCallFailedException.class);

            then(llmCallRepository).should(never()).save(any(LlmCall.class));
        }

        @Test
        @DisplayName("throws when input snapshot serialization fails")
        void generate_fail_whenInputSnapshotSerializationFails() throws Exception {
            // given
            UUID llmId = UUID.randomUUID();
            Long actorId = 1L;
            LlmInputSnapshot llmInputSnapshot = new LlmInputSnapshot("prompt", "Americano", 4500, null);
            Llm activeLlm = createLlm(llmId, "gpt-5.4-mini", LlmProvider.OPENAI, true);

            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(activeLlm));
            given(objectMapper.writeValueAsString(any(LlmInputSnapshot.class)))
                    .willThrow(new JsonProcessingException("serialize fail") { });

            // when // then
            assertThatThrownBy(() -> llmOrchestrator.generate(actorId, llmInputSnapshot))
                    .isInstanceOf(LlmInputSnapshotSerializationException.class);

            then(llmClientRegistry).shouldHaveNoInteractions();
            then(llmCallRepository).shouldHaveNoInteractions();
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
