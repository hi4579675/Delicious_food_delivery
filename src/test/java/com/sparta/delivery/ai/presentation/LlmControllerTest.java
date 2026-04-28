package com.sparta.delivery.ai.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.application.LlmService;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.presentation.dto.request.LlmCreateRequest;
import com.sparta.delivery.ai.presentation.dto.request.LlmUpdateRequest;
import com.sparta.delivery.ai.presentation.dto.response.LlmResponse;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LlmController.class)
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LlmService llmService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("LLM management API")
    class LlmApi {

        @Test
        @DisplayName("manager can create llm")
        void createLlm_success() throws Exception {
            UUID llmId = UUID.randomUUID();
            LlmCreateRequest request = new LlmCreateRequest("gpt-4.1-mini", LlmProvider.OPENAI);
            LlmResponse response = llmResponse(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);

            given(llmService.create(eq(1L), eq(UserRole.MANAGER), eq(request)))
                    .willReturn(response);

            mockMvc.perform(post("/api/v1/llms")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.llmId").value(llmId.toString()))
                    .andExpect(jsonPath("$.data.llmName").value("gpt-4.1-mini"));

            then(llmService).should().create(eq(1L), eq(UserRole.MANAGER), any(LlmCreateRequest.class));
        }

        @Test
        @DisplayName("returns 400 when llmName is blank")
        void createLlm_fail_whenLlmNameBlank() throws Exception {
            LlmCreateRequest request = new LlmCreateRequest("", LlmProvider.OPENAI);

            mockMvc.perform(post("/api/v1/llms")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(llmService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("manager can read llm detail")
        void getLlm_success() throws Exception {
            UUID llmId = UUID.randomUUID();
            LlmResponse response = llmResponse(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);

            given(llmService.getLlm(UserRole.MANAGER, llmId)).willReturn(response);

            mockMvc.perform(get("/api/v1/llms/{llmId}", llmId)
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.llmId").value(llmId.toString()))
                    .andExpect(jsonPath("$.data.llmName").value("gpt-4.1-mini"));

            then(llmService).should().getLlm(UserRole.MANAGER, llmId);
        }

        @Test
        @DisplayName("manager can read llm list")
        void getLlms_success() throws Exception {
            LlmResponse item = llmResponse(UUID.randomUUID(), "gpt-4.1-mini", LlmProvider.OPENAI, false);

            given(llmService.getLlms(UserRole.MANAGER, 0, 10, null, null))
                    .willReturn(new PageImpl<>(List.of(item)));

            mockMvc.perform(get("/api/v1/llms")
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].llmName").value("gpt-4.1-mini"));

            then(llmService).should().getLlms(UserRole.MANAGER, 0, 10, null, null);
        }

        @Test
        @DisplayName("manager can change llm name")
        void changeLlmName_success() throws Exception {
            UUID llmId = UUID.randomUUID();
            LlmUpdateRequest request = new LlmUpdateRequest("gpt-4.1");
            LlmResponse response = llmResponse(llmId, "gpt-4.1", LlmProvider.OPENAI, false);

            given(llmService.changeLlmName(eq(1L), eq(UserRole.MANAGER), eq(llmId), eq(request)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/v1/llms/{llmId}", llmId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.llmName").value("gpt-4.1"));

            then(llmService).should().changeLlmName(eq(1L), eq(UserRole.MANAGER), eq(llmId), any(LlmUpdateRequest.class));
        }

        @Test
        @DisplayName("manager can activate llm")
        void activateLlm_success() throws Exception {
            UUID llmId = UUID.randomUUID();
            LlmResponse response = llmResponse(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, true);

            given(llmService.activate(1L, UserRole.MANAGER, llmId)).willReturn(response);

            mockMvc.perform(patch("/api/v1/llms/{llmId}/activate", llmId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.llmId").value(llmId.toString()))
                    .andExpect(jsonPath("$.data.isActive").value(true));

            then(llmService).should().activate(1L, UserRole.MANAGER, llmId);
        }

        @Test
        @DisplayName("manager can delete llm")
        void deleteLlm_success() throws Exception {
            UUID llmId = UUID.randomUUID();
            LlmResponse response = llmResponse(llmId, "gpt-4.1-mini", LlmProvider.OPENAI, false);

            given(llmService.delete(1L, UserRole.MANAGER, llmId)).willReturn(response);

            mockMvc.perform(delete("/api/v1/llms/{llmId}", llmId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.llmId").value(llmId.toString()));

            then(llmService).should().delete(1L, UserRole.MANAGER, llmId);
        }
    }

    private UsernamePasswordAuthenticationToken authenticationToken(UserRole role) {
        TestUserPrincipal principal = new TestUserPrincipal(1L, "user@test.com", role.name());
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private LlmResponse llmResponse(UUID llmId, String llmName, LlmProvider provider, boolean isActive) {
        return new LlmResponse(
                llmId,
                llmName,
                provider,
                isActive,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private record TestUserPrincipal(
            Long id,
            String username,
            String role
    ) implements UserPrincipal {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getRole() {
            return role;
        }
    }
}
