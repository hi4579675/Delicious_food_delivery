package com.sparta.delivery.review.presentation;

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

import java.util.List;
import java.util.UUID;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.review.application.service.ReviewService;
import com.sparta.delivery.review.presentation.dto.request.ReviewCreateRequest;
import com.sparta.delivery.review.presentation.dto.request.ReviewUpdateRequest;
import com.sparta.delivery.review.presentation.dto.response.ReviewResponse;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.UserRole;

@WebMvcTest(ReviewController.class)
@Import(ReviewControllerTest.TestSecurityConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(reviewService, jwtProvider, userService);
    }

    @Nested
    @DisplayName("리뷰 생성 API")
    class CreateApi {

        @Test
        @DisplayName("CUSTOMER 권한이면 리뷰를 생성한다")
        void create_success() throws Exception {
            // given
            UUID orderId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();

            ReviewCreateRequest request = new ReviewCreateRequest(orderId, 5, "맛있어요");
            ReviewResponse response = new ReviewResponse(reviewId, orderId, storeId, 1L, 5, "맛있어요");

            given(reviewService.create(1L, UserRole.CUSTOMER, request)).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/reviews")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                    .andExpect(jsonPath("$.data.orderId").value(orderId.toString()));

            then(reviewService).should().create(1L, UserRole.CUSTOMER, request);
        }

        @Test
        @DisplayName("요청 본문 검증 실패면 400을 반환한다")
        void create_fail_whenValidationFails() throws Exception {
            // given
            String invalidBody = """
                    {
                      "rating": 5,
                      "content": "맛있어요"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/reviews")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());

            then(reviewService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 403을 반환한다")
        void create_forbidden_whenNotCustomer() throws Exception {
            // given
            ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), 5, "맛있어요");

            // when & then
            mockMvc.perform(post("/api/v1/reviews")
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(reviewService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("리뷰 조회 API")
    class ReadApi {

        @Test
        @DisplayName("권한이 있으면 리뷰 단건을 조회한다")
        void getByReviewId_success() throws Exception {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            ReviewResponse response = new ReviewResponse(reviewId, orderId, storeId, 1L, 4, "good");

            given(reviewService.getByReviewId(reviewId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/reviews/{reviewId}", reviewId)
                            .with(authentication(authenticationToken(UserRole.MANAGER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()));

            then(reviewService).should().getByReviewId(reviewId);
        }

        @Test
        @DisplayName("가게 리뷰 목록을 조회한다")
        void getReviews_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ReviewResponse review = new ReviewResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    storeId,
                    1L,
                    5,
                    "great"
            );
            PageResponse<ReviewResponse> pageResponse = new PageResponse<>(
                    List.of(review),
                    0,
                    10,
                    1,
                    1,
                    true
            );

            given(reviewService.getReviews(storeId, 0, 10, "createdAt", "desc")).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/reviews")
                            .param("storeId", storeId.toString())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            then(reviewService).should().getReviews(storeId, 0, 10, "createdAt", "desc");
        }
    }

    @Nested
    @DisplayName("리뷰 수정 API")
    class UpdateApi {

        @Test
        @DisplayName("CUSTOMER 권한이면 리뷰를 수정한다")
        void update_success() throws Exception {
            // given
            UUID reviewId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            ReviewUpdateRequest request = new ReviewUpdateRequest(3, "보통이에요");
            ReviewResponse response = new ReviewResponse(reviewId, orderId, storeId, 1L, 3, "보통이에요");

            given(reviewService.update(1L, reviewId, UserRole.CUSTOMER, request)).willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                    .andExpect(jsonPath("$.data.rating").value(3));

            then(reviewService).should().update(1L, reviewId, UserRole.CUSTOMER, request);
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 403을 반환한다")
        void update_forbidden_whenNotCustomer() throws Exception {
            // given
            UUID reviewId = UUID.randomUUID();
            ReviewUpdateRequest request = new ReviewUpdateRequest(4, "좋아요");

            // when & then
            mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.MANAGER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(reviewService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 API")
    class DeleteApi {

        @Test
        @DisplayName("CUSTOMER 권한이면 리뷰를 삭제한다")
        void delete_success() throws Exception {
            // given
            UUID reviewId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.CUSTOMER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(reviewService).should().delete(1L, reviewId, UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("CUSTOMER가 아니면 403을 반환한다")
        void delete_forbidden_whenNotCustomer() throws Exception {
            // given
            UUID reviewId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER))))
                    .andExpect(status().isForbidden());

            then(reviewService).shouldHaveNoInteractions();
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
