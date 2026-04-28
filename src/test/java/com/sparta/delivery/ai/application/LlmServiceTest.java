package com.sparta.delivery.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private LlmRepository llmRepository;

    @InjectMocks
    private LlmService llmService;

    @Nested
    @DisplayName("LLM create")
    class Create {

        @Test
        @DisplayName("manager can create llm")
        void create_success() {
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

            var response = llmService.create(actorId, UserRole.MANAGER, request);

            assertThat(response.llmName()).isEqualTo("gpt-4.1-mini");
            assertThat(response.provider()).isEqualTo(LlmProvider.OPENAI);
            assertThat(response.isActive()).isFalse();
            then(llmRepository).should().save(any(Llm.class));
        }

        @Test
        @DisplayName("cannot create duplicated llm name")
        void create_fail_duplicateName() {
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1-mini")).willReturn(true);

            assertThatThrownBy(() -> llmService.create(1L, UserRole.MANAGER, request))
                    .isInstanceOf(DuplicateLlmNameException.class);
            then(llmRepository).should(never()).save(any(Llm.class));
        }

        @Test
        @DisplayName("customer cannot create llm")
        void create_fail_forbidden() {
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);

            assertThatThrownBy(() -> llmService.create(1L, UserRole.CUSTOMER, request))
                    .isInstanceOf(AiForbiddenException.class);
            then(llmRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("LLM read")
    class Read {

        @Test
        @DisplayName("can read llm detail")
        void getLlm_success() {
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, true);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

            var response = llmService.getLlm(UserRole.MANAGER, llmId);

            assertThat(response.llmId()).isEqualTo(llmId);
            assertThat(response.llmName()).isEqualTo("gpt-4.1-mini");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws when llm does not exist")
        void getLlm_fail_notFound() {
            UUID llmId = UUID.randomUUID();
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> llmService.getLlm(UserRole.MANAGER, llmId))
                    .isInstanceOf(LlmNotFoundException.class);
        }

        @Test
        @DisplayName("reads llm list with pagination")
        void getLlms_success() {
            Llm active = createLlm(UUID.randomUUID(), "gpt-4.1", LlmProvider.OPENAI, true);
            Llm inactive = createLlm(UUID.randomUUID(), "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(active, inactive)));

            Page<com.sparta.delivery.ai.presentation.dto.response.LlmResponse> responses =
                    llmService.getLlms(UserRole.MANAGER, 0, 10, null, null);

            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getContent().get(0).isActive()).isTrue();
            assertThat(responses.getContent().get(1).isActive()).isFalse();
        }

        @Test
        @DisplayName("reads llm list filtered by keyword")
        void getLlms_success_withKeyword() {
            Llm llm = createLlm(UUID.randomUUID(), "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmNameContainingIgnoreCase(eq("mini"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(llm)));

            Page<com.sparta.delivery.ai.presentation.dto.response.LlmResponse> responses =
                    llmService.getLlms(UserRole.MANAGER, 0, 10, null, "mini");

            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).llmName()).isEqualTo("gpt-4.1-mini");
        }
    }

    @Nested
    @DisplayName("LLM update")
    class Update {

        @Test
        @DisplayName("can change llm name")
        void changeLlmName_success() {
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1")).willReturn(false);

            var response = llmService.changeLlmName(1L, UserRole.MANAGER, llmId, new LlmUpdateRequest("gpt-4.1"));

            assertThat(response.llmName()).isEqualTo("gpt-4.1");
        }

        @Test
        @DisplayName("throws when changed llm name is duplicated")
        void changeLlmName_fail_duplicateName() {
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));
            given(llmRepository.existsIncludingDeletedByLlmName("gpt-4.1")).willReturn(true);

            assertThatThrownBy(() -> llmService.changeLlmName(1L, UserRole.MANAGER, llmId, new LlmUpdateRequest("gpt-4.1")))
                    .isInstanceOf(DuplicateLlmNameException.class);
        }
    }

    @Nested
    @DisplayName("LLM activate")
    class Activate {

        @Test
        @DisplayName("activating an inactive llm deactivates current active llm")
        void activate_success() {
            UUID targetId = UUID.randomUUID();
            UUID activeId = UUID.randomUUID();
            Llm currentActive = createLlm(activeId, "gpt-4.1-mini", LlmProvider.OPENAI, true);
            Llm target = createLlm(targetId, "gpt-4.1", LlmProvider.OPENAI, false);

            given(llmRepository.findByLlmId(targetId)).willReturn(Optional.of(target));
            given(llmRepository.findByIsActiveTrue()).willReturn(Optional.of(currentActive));

            var response = llmService.activate(1L, UserRole.MANAGER, targetId);

            assertThat(response.llmId()).isEqualTo(targetId);
            assertThat(response.isActive()).isTrue();
            assertThat(target.isActive()).isTrue();
            assertThat(currentActive.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("LLM delete")
    class Delete {

        @Test
        @DisplayName("soft deletes an inactive llm")
        void delete_success() {
            UUID llmId = UUID.randomUUID();
            Long actorId = 1L;
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

            var response = llmService.delete(actorId, UserRole.MANAGER, llmId);

            assertThat(response.llmId()).isEqualTo(llmId);
            assertThat(llm.isDeleted()).isTrue();
            assertThat(llm.getDeletedBy()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("cannot delete active llm")
        void delete_fail_activeLlm() {
            UUID llmId = UUID.randomUUID();
            Llm llm = createLlm(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, true);
            given(llmRepository.findByLlmId(llmId)).willReturn(Optional.of(llm));

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
