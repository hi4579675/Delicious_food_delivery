package com.sparta.delivery.ai.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sparta.delivery.ai.application.LlmCallService;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallListResponse;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LlmCallController.class)
class LlmCallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LlmCallService llmCallService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("LLM 호출 로그 API")
    class LlmCallApi {

        @Test
        @DisplayName("MANAGER 권한이면 호출 로그 단건을 조회한다")
        void getLlmCall_success() throws Exception {
            UUID callId = UUID.randomUUID();
            LlmCallResponse response = new LlmCallResponse(
                    callId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "{\"productName\":\"Americano\"}",
                    "STOP",
                    "{\"result\":\"ok\"}",
                    "generated description",
                    LocalDateTime.now(),
                    1L
            );

            given(llmCallService.getLlmCall(UserRole.MANAGER, callId)).willReturn(response);

            mockMvc.perform(get("/api/v1/llm-calls/{callId}", callId)
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.callId").value(callId.toString()))
                    .andExpect(jsonPath("$.data.finishReason").value("STOP"));

            then(llmCallService).should().getLlmCall(UserRole.MANAGER, callId);
        }

        @Test
        @DisplayName("MANAGER 권한이면 호출 로그 목록을 조회한다")
        void getLlmCalls_success() throws Exception {
            LlmCallListResponse response = new LlmCallListResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "STOP",
                    LocalDateTime.now()
            );

            given(llmCallService.getLlmCalls(UserRole.MANAGER)).willReturn(List.of(response));

            mockMvc.perform(get("/api/v1/llm-calls")
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].finishReason").value("STOP"));

            then(llmCallService).should().getLlmCalls(UserRole.MANAGER);
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
