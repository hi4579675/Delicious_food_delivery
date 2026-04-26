package com.sparta.delivery.ai.application;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.AiForbiddenException;
import com.sparta.delivery.ai.domain.exception.CannotDeleteActiveLlmException;
import com.sparta.delivery.ai.domain.exception.DuplicateLlmNameException;
import com.sparta.delivery.ai.domain.exception.LlmNotFoundException;
import com.sparta.delivery.ai.domain.repository.LlmRepository;
import com.sparta.delivery.ai.presentation.dto.request.LlmCreateRequest;
import com.sparta.delivery.ai.presentation.dto.request.LlmUpdateRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private LlmRepository llmRepository;

    @InjectMocks
    private LlmService llmService;

    @Nested
    @DisplayName("LLM 생성")
    class Create {

        @Test
        @DisplayName("MANAGER는 LLM을 생성할 수 있다")
        void create_success() {
            // given
            Long actorId = 1L;
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1-mini")).willReturn(false);
            given(llmRepository.save(any(Llm.class))).willAnswer(invocation -> {
                Llm llm = invocation.getArgument(0);
                ReflectionTestUtils.setField(llm, "llmId", UUID.randomUUID());
                ReflectionTestUtils.setField(llm, "createdAt", LocalDateTime.now());
                ReflectionTestUtils.setField(llm, "updatedAt", LocalDateTime.now());
                return llm;
            });

            // when
            var response = llmService.create(actorId, UserRole.MANAGER, request);

            // then
            assertThat(response.llmName()).isEqualTo("gpt-4.1-mini");
            assertThat(response.provider()).isEqualTo(LlmProvider.OPENAI);
            assertThat(response.isActive()).isFalse();
            then(llmRepository).should().save(any(Llm.class));
        }

        @Test
        @DisplayName("중복된 LLM 이름이면 생성할 수 없다")
        void create_fail_duplicateName() {
            // given
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1-mini")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> llmService.create(1L, UserRole.MANAGER, request))
                    .isInstanceOf(DuplicateLlmNameException.class);
            then(llmRepository).should(never()).save(any(Llm.class));
        }

        @Test
        @DisplayName("권한이 없으면 생성할 수 없다")
        void create_fail_forbidden() {
            // given
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);

            // when & then
            assertThatThrownBy(() -> llmService.create(1L, UserRole.CUSTOMER, request))
                    .isInstanceOf(AiForbiddenException.class);
            then(llmRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("LLM 조회")
    class Read {

        @Test
        @DisplayName("단건 조회에 성공한다")
        void getLlm_success() {
            // given
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, true);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

            // when
            var response = llmService.getLlm(UserRole.MANAGER, llmId);

            // then
            assertThat(response.llmId()).isEqualTo(llmId);
            assertThat(response.llmName()).isEqualTo("gpt-4.1-mini");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않으면 예외가 발생한다")
        void getLlm_fail_notFound() {
            // given
            UUID llmId = UUID.randomUUID();
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> llmService.getLlm(UserRole.MANAGER, llmId))
                    .isInstanceOf(LlmNotFoundException.class);
        }

        @Test
        @DisplayName("목록 조회는 활성순, 수정일순 정렬로 조회한다")
        void getLlms_success() {
            // given
            Llm active = createLlm(UUID.randomUUID(), "gpt-4.1", LlmProvider.OPENAI, true);
            Llm inactive = createLlm(UUID.randomUUID(), "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findAll(eq(Sort.by(
                    Sort.Order.desc("isActive"),
                    Sort.Order.desc("updatedAt")
            )))).willReturn(List.of(active, inactive));

            // when
            var responses = llmService.getLlms(UserRole.MANAGER);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).isActive()).isTrue();
            assertThat(responses.get(1).isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("LLM 이름 변경")
    class Update {

        @Test
        @DisplayName("이름을 변경할 수 있다")
        void changeLlmName_success() {
            // given
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1")).willReturn(false);

            // when
            var response = llmService.changeLlmName(1L, UserRole.MANAGER, llmId, new LlmUpdateRequest("gpt-4.1"));

            // then
            assertThat(response.llmName()).isEqualTo("gpt-4.1");
        }

        @Test
        @DisplayName("변경한 이름이 중복되면 예외가 발생한다")
        void changeLlmName_fail_duplicateName() {
            // given
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> llmService.changeLlmName(1L, UserRole.MANAGER, llmId, new LlmUpdateRequest("gpt-4.1")))
                    .isInstanceOf(DuplicateLlmNameException.class);
        }
    }

    @Nested
    @DisplayName("LLM 삭제")
    class Delete {

        @Test
        @DisplayName("비활성 LLM은 soft delete할 수 있다")
        void delete_success() {
            // given
            UUID llmId = UUID.randomUUID();
            Long actorId = 1L;
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

            // when
            var response = llmService.delete(actorId, UserRole.MANAGER, llmId);

            // then
            assertThat(response.llmId()).isEqualTo(llmId);
            assertThat(llm.isDeleted()).isTrue();
            assertThat(llm.getDeletedBy()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("활성 LLM은 삭제할 수 없다")
        void delete_fail_activeLlm() {
            // given
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, true);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

            // when & then
            assertThatThrownBy(() -> llmService.delete(1L, UserRole.MANAGER, llmId))
                    .isInstanceOf(CannotDeleteActiveLlmException.class);
            assertThat(llm.isDeleted()).isFalse();
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
