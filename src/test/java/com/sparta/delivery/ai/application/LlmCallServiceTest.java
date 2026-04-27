package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.LlmCall;
import com.sparta.delivery.ai.domain.exception.AiForbiddenException;
import com.sparta.delivery.ai.domain.exception.LlmCallNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmCallRepository;
import com.sparta.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LlmCallServiceTest {

    @Mock
    private LlmCallRepository llmCallRepository;

    @InjectMocks
    private LlmCallService llmCallService;

    @Nested
    @DisplayName("LLM 호출 로그 단건 조회")
    class ReadOne {

        @Test
        @DisplayName("단건 조회에 성공한다")
        void getLlmCall_success() {
            // given
            UUID callId = UUID.randomUUID();
            LlmCall llmCall = createLlmCall(callId);
            given(llmCallRepository.findByCallId(callId)).willReturn(Optional.of(llmCall));

            // when
            var response = llmCallService.getLlmCall(UserRole.MANAGER, callId);

            // then
            assertThat(response.callId()).isEqualTo(callId);
            assertThat(response.inputSnapshot()).isEqualTo("{\"productName\":\"Americano\"}");
            assertThat(response.providerStatusCode()).isEqualTo("200");
        }

        @Test
        @DisplayName("존재하지 않으면 예외가 발생한다")
        void getLlmCall_fail_notFound() {
            // given
            UUID callId = UUID.randomUUID();
            given(llmCallRepository.findByCallId(callId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> llmCallService.getLlmCall(UserRole.MANAGER, callId))
                    .isInstanceOf(LlmCallNotFoundException.class);
        }

        @Test
        @DisplayName("권한이 없으면 조회할 수 없다")
        void getLlmCall_fail_forbidden() {
            // when & then
            assertThatThrownBy(() -> llmCallService.getLlmCall(UserRole.CUSTOMER, UUID.randomUUID()))
                    .isInstanceOf(AiForbiddenException.class);
            then(llmCallRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("LLM 호출 로그 목록 조회")
    class ReadMany {

        @Test
        @DisplayName("생성일 내림차순 정렬로 목록 조회에 성공한다")
        void getLlmCalls_success() {
            // given
            LlmCall recent = createLlmCall(UUID.randomUUID());
            LlmCall old = createLlmCall(UUID.randomUUID());
            given(llmCallRepository.findAll(eq(Sort.by(
                    Sort.Order.desc("createdAt")
            )))).willReturn(List.of(recent, old));

            // when
            var responses = llmCallService.getLlmCalls(UserRole.MANAGER);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).callId()).isEqualTo(recent.getCallId());
            assertThat(responses.get(1).callId()).isEqualTo(old.getCallId());
        }

        @Test
        @DisplayName("권한이 없으면 목록 조회할 수 없다")
        void getLlmCalls_fail_forbidden() {
            // when & then
            assertThatThrownBy(() -> llmCallService.getLlmCalls(UserRole.CUSTOMER))
                    .isInstanceOf(AiForbiddenException.class);
            then(llmCallRepository).shouldHaveNoInteractions();
        }
    }

    private LlmCall createLlmCall(UUID callId) {
        LlmCall llmCall = LlmCall.create(
                UUID.randomUUID(),
                "{\"productName\":\"Americano\"}",
                "200",
                "{\"result\":\"ok\"}",
                "generated description",
                1L
        );
        ReflectionTestUtils.setField(llmCall, "callId", callId);
        ReflectionTestUtils.setField(llmCall, "createdAt", LocalDateTime.now());
        return llmCall;
    }
}
