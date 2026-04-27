package com.sparta.delivery.store.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationFilter;
import com.sparta.delivery.auth.infrastructure.jwt.JwtProvider;
import com.sparta.delivery.common.config.security.SecurityConfig;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.store.application.StoreService;
import com.sparta.delivery.store.application.dto.StoreSearchCondition;
import com.sparta.delivery.store.presentation.dto.StoreCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreResponse;
import com.sparta.delivery.store.presentation.dto.StoreUpdateRequest;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
@Import(SecurityConfig.class)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Nested
    @DisplayName("가게 생성 API")
    class CreateStoreApiTest {

        @Test
        @DisplayName("OWNER 권한이면 가게를 생성한다")
        void createStore_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            StoreResponse response = new StoreResponse(
                    storeId,
                    regionId,
                    categoryId,
                    1L,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true,
                    BigDecimal.ZERO,
                    0
            );

            given(storeService.createStore(eq(1L), any(StoreCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/stores")
                            .with(csrf())
                            .with(authentication(ownerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.storeName").value("왕조치킨"));

            then(storeService).should().createStore(eq(1L), any(StoreCreateRequest.class));
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게를 생성할 수 없다")
        void createStore_fail_whenCustomer() throws Exception {
            // given
            StoreCreateRequest request = new StoreCreateRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            // when & then
            mockMvc.perform(post("/api/v1/stores")
                            .with(csrf())
                            .with(authentication(customerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(storeService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("가게 조회 API")
    class GetStoreApiTest {

        @Test
        @DisplayName("조건 없이 전체 가게 목록을 조회한다")
        void getStores_success() throws Exception {
            // given
            StoreResponse response = new StoreResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1L,
                    "왕조치킨",
                    "설명",
                    "주소",
                    "상세주소",
                    "02-1234-5678",
                    15000,
                    true,
                    true,
                    BigDecimal.ZERO,
                    0
            );

            given(storeService.searchStores(any(StoreSearchCondition.class), any(Pageable.class), any(UserRole.class)))
                    .willReturn(new PageResponse<>(List.of(response), 0, 10, 1, 1, true));

            // when & then
            mockMvc.perform(get("/api/v1/stores")
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeName").value("왕조치킨"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10));

            then(storeService).should().searchStores(
                    eq(new StoreSearchCondition(
                            null, null, null, null, null, null, null, null, null, null, null, null
                    )),
                    eq(PageRequest.of(0, 10, org.springframework.data.domain.Sort.Direction.DESC, "createdAt")),
                    eq(UserRole.CUSTOMER)
            );
        }

        @Test
        @DisplayName("관리자는 활성 여부를 포함한 검색 조건으로 가게 목록을 조회한다")
        void getStores_success_withConditions() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            LocalDateTime createdAfter = LocalDateTime.of(2026, 4, 1, 0, 0);
            LocalDateTime createdBefore = LocalDateTime.of(2026, 4, 30, 23, 59);

            given(storeService.searchStores(any(StoreSearchCondition.class), any(Pageable.class), any(UserRole.class)))
                    .willReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

            // when & then
            mockMvc.perform(get("/api/v1/stores")
                            .with(authentication(managerAuthentication()))
                            .param("regionId", regionId.toString())
                            .param("categoryId", categoryId.toString())
                            .param("isOpen", "true")
                            .param("isActive", "false")
                            .param("keyword", "치킨")
                            .param("addressKeyword", "종로구")
                            .param("minRating", "4.0")
                            .param("minReviewCount", "10")
                            .param("maxMinOrderAmount", "20000")
                            .param("createdAfter", "2026-04-01T00:00:00")
                            .param("createdBefore", "2026-04-30T23:59:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(storeService).should().searchStores(
                    eq(new StoreSearchCondition(
                            regionId,
                            categoryId,
                            null,
                            true,
                            false,
                            "치킨",
                            "종로구",
                            BigDecimal.valueOf(4.0),
                            10,
                            20000,
                            createdAfter,
                            createdBefore
                    )),
                    eq(PageRequest.of(0, 10, org.springframework.data.domain.Sort.Direction.DESC, "createdAt")),
                    eq(UserRole.MANAGER)
            );
        }

        @Test
        @DisplayName("사용자 조건으로 가게 목록을 조회한다")
        void getStores_success_withUserId() throws Exception {
            // given
            Long userId = 1L;
            StoreResponse response = new StoreResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    userId,
                    "왕조치킨",
                    "설명",
                    "주소",
                    "상세주소",
                    "02-1234-5678",
                    15000,
                    true,
                    true,
                    BigDecimal.ZERO,
                    0
            );

            given(storeService.searchStores(any(StoreSearchCondition.class), any(Pageable.class), any(UserRole.class)))
                    .willReturn(new PageResponse<>(List.of(response), 0, 10, 1, 1, true));

            // when & then
            mockMvc.perform(get("/api/v1/stores")
                            .with(authentication(customerAuthentication()))
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].userId").value(userId));

            then(storeService).should().searchStores(
                    eq(new StoreSearchCondition(
                            null, null, userId, null, null, null, null, null, null, null, null, null
                    )),
                    eq(PageRequest.of(0, 10, org.springframework.data.domain.Sort.Direction.DESC, "createdAt")),
                    eq(UserRole.CUSTOMER)
            );
        }

        @Test
        @DisplayName("가게를 단건 조회한다")
        void getStore_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreResponse response = new StoreResponse(
                    storeId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1L,
                    "왕조치킨",
                    "설명",
                    "주소",
                    "상세주소",
                    "02-1234-5678",
                    15000,
                    true,
                    true,
                    BigDecimal.ZERO,
                    0
            );

            given(storeService.getStore(storeId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/stores/{storeId}", storeId)
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.storeName").value("왕조치킨"));

            then(storeService).should().getStore(storeId);
        }
    }

    @Nested
    @DisplayName("가게 수정 API")
    class UpdateStoreApiTest {

        @Test
        @DisplayName("MANAGER 권한이면 가게를 수정한다")
        void updateStore_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreUpdateRequest request = new StoreUpdateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            StoreResponse response = new StoreResponse(
                    storeId,
                    regionId,
                    categoryId,
                    1L,
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true,
                    BigDecimal.ZERO,
                    0
            );

            given(storeService.updateStore(
                    eq(storeId),
                    eq(1L),
                    eq(UserRole.MANAGER),
                    any(StoreUpdateRequest.class)
            )).willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/stores/{storeId}", storeId)
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("왕조치킨 수정"));

            then(storeService).should().updateStore(
                    eq(storeId),
                    eq(1L),
                    eq(UserRole.MANAGER),
                    any(StoreUpdateRequest.class)
            );
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게를 수정할 수 없다")
        void updateStore_fail_whenCustomer() throws Exception {
            // given
            StoreUpdateRequest request = new StoreUpdateRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            // when & then
            mockMvc.perform(put("/api/v1/stores/{storeId}", UUID.randomUUID())
                            .with(csrf())
                            .with(authentication(customerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(storeService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("가게 삭제 API")
    class DeleteStoreApiTest {

        @Test
        @DisplayName("MASTER 권한이면 가게를 삭제한다")
        void deleteStore_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/stores/{storeId}", storeId)
                            .with(csrf())
                            .with(authentication(masterAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(storeService).should().deleteStore(storeId, 1L, UserRole.MASTER);
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게를 삭제할 수 없다")
        void deleteStore_fail_whenCustomer() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/stores/{storeId}", UUID.randomUUID())
                            .with(csrf())
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isForbidden());

            then(storeService).shouldHaveNoInteractions();
        }
    }

    private UsernamePasswordAuthenticationToken ownerAuthentication() {
        return createAuthentication("OWNER", "ROLE_OWNER");
    }

    private UsernamePasswordAuthenticationToken managerAuthentication() {
        return createAuthentication("MANAGER", "ROLE_MANAGER");
    }

    private UsernamePasswordAuthenticationToken masterAuthentication() {
        return createAuthentication("MASTER", "ROLE_MASTER");
    }

    private UsernamePasswordAuthenticationToken customerAuthentication() {
        return createAuthentication("CUSTOMER", "ROLE_CUSTOMER");
    }

    private UsernamePasswordAuthenticationToken createAuthentication(String role, String authority) {
        TestUserPrincipal principal = new TestUserPrincipal(1L, role.toLowerCase(), role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
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
