package com.sparta.delivery.payment.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.payment.application.service.PaymentService;
import com.sparta.delivery.payment.domain.entity.PaymentFailureReason;
import com.sparta.delivery.payment.domain.entity.PaymentMethod;
import com.sparta.delivery.payment.domain.entity.PaymentStatus;
import com.sparta.delivery.payment.presentation.dto.PaymentCreateRequest;
import com.sparta.delivery.payment.presentation.dto.PaymentResponse;
import com.sparta.delivery.payment.presentation.dto.PaymentStatusUpdateRequest;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(PaymentControllerTest.TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(paymentService, jwtProvider, userService);
    }

    @Nested
    @DisplayName("결제 생성 API")
    class CreatePaymentApi {

        @Test
        @DisplayName("CUSTOMER 권한이면 결제를 생성한다")
        void createPayment_success() throws Exception {
            // given
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);
            PaymentResponse response = paymentResponse(UUID.randomUUID(), orderId, 10_000);

            given(paymentService.create(eq(1L), eq(request))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.totalPrice").value(10_000));

            then(paymentService).should().create(eq(1L), eq(request));
        }

        @Test
        @DisplayName("orderId가 없으면 400을 반환한다")
        void createPayment_fail_whenOrderIdNull() throws Exception {
            // given
            String invalidBody = """
                    {
                      "paymentMethod": "CARD",
                      "totalPrice": 10000
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/payments")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());

            then(paymentService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("결제 단건 조회 API")
    class GetPaymentApi {

        @Test
        @DisplayName("MANAGER 권한이면 결제를 단건 조회한다")
        void getPayment_success() throws Exception {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentResponse response = paymentResponse(paymentId, UUID.randomUUID(), 10_000);

            given(paymentService.getByPaymentId(1L, UserRole.MANAGER, paymentId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId)
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()));

            then(paymentService).should().getByPaymentId(1L, UserRole.MANAGER, paymentId);
        }
    }

    @Nested
    @DisplayName("결제 목록 조회 API")
    class GetPaymentsApi {

        @Test
        @DisplayName("MANAGER 권한이면 결제 목록을 조회한다")
        void getPayments_success() throws Exception {
            // given
            PaymentResponse response = paymentResponse(UUID.randomUUID(), UUID.randomUUID(), 10_000);
            given(paymentService.getPayments(1L, UserRole.MANAGER, 0, 10, "createdAt", "desc"))
                    .willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/payments")
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].totalPrice").value(10_000));

            then(paymentService).should().getPayments(1L, UserRole.MANAGER, 0, 10, "createdAt", "desc");
        }

        @Test
        @DisplayName("size가 10/30/50이 아니면 10으로 보정한다")
        void getPayments_success_sizeNormalized() throws Exception {
            // given
            given(paymentService.getPayments(1L, UserRole.MANAGER, 0, 10, "createdAt", "desc"))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/payments")
                            .param("page", "0")
                            .param("size", "999")
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk());

            then(paymentService).should().getPayments(1L, UserRole.MANAGER, 0, 10, "createdAt", "desc");
        }
    }

    @Nested
    @DisplayName("결제 삭제 API")
    class DeletePaymentApi {

        @Test
        @DisplayName("MASTER 권한이면 결제를 삭제한다")
        void deletePayment_success() throws Exception {
            // given
            UUID paymentId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/payments/{paymentId}", paymentId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MASTER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(paymentService).should().delete(1L, UserRole.MASTER, paymentId);
        }

        @Test
        @DisplayName("MASTER가 아니면 403을 반환한다")
        void deletePayment_forbidden_whenNotMaster() throws Exception {
            // given
            UUID paymentId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/payments/{paymentId}", paymentId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER))))
                    .andExpect(status().isForbidden());

            then(paymentService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("결제 상태 변경 API")
    class UpdatePaymentStatusApi {

        @Test
        @DisplayName("MASTER 권한이면 결제 상태를 변경한다")
        void updateStatus_success() throws Exception {
            // given
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(
                    PaymentStatus.FAILED,
                    PaymentFailureReason.PG_TIMEOUT,
                    null,
                    null
            );
            PaymentResponse response = paymentResponse(paymentId, orderId, 10_000);

            given(paymentService.updateStatus(eq(1L), eq(paymentId), any(PaymentStatusUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/payments/{paymentId}/status", paymentId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MASTER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()));

            then(paymentService).should().updateStatus(eq(1L), eq(paymentId), any(PaymentStatusUpdateRequest.class));
        }

        @Test
        @DisplayName("MASTER가 아니면 403을 반환한다")
        void updateStatus_forbidden_whenNotMaster() throws Exception {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(
                    PaymentStatus.FAILED,
                    PaymentFailureReason.PG_TIMEOUT,
                    null,
                    null
            );

            // when & then
            mockMvc.perform(put("/api/v1/payments/{paymentId}/status", paymentId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(paymentService).shouldHaveNoInteractions();
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

    private PaymentResponse paymentResponse(UUID paymentId, UUID orderId, int totalPrice) {
        return new PaymentResponse(
                paymentId,
                orderId,
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                totalPrice,
                null,
                null,
                null,
                null,
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

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
