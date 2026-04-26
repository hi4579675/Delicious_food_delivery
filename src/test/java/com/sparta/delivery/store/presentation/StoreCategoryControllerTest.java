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
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.store.application.StoreCategoryService;
import com.sparta.delivery.store.presentation.dto.StoreCategoryCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreCategoryResponse;
import com.sparta.delivery.store.presentation.dto.StoreCategoryUpdateRequest;
import com.sparta.delivery.user.application.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreCategoryController.class)
@Import(SecurityConfig.class)
class StoreCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoreCategoryService storeCategoryService;

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
    @DisplayName("가게 카테고리 생성 API")
    class CreateCategoryApiTest {

        @Test
        @DisplayName("MANAGER 권한이면 가게 카테고리를 생성한다")
        void createCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategoryCreateRequest request = new StoreCategoryCreateRequest(
                    "치킨",
                    "치킨 카테고리",
                    true
            );
            StoreCategoryResponse response = new StoreCategoryResponse(
                    categoryId,
                    "치킨",
                    "치킨 카테고리",
                    1,
                    true
            );

            given(storeCategoryService.createCategory(any(StoreCategoryCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/store-categories")
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.categoryId").value(categoryId.toString()))
                    .andExpect(jsonPath("$.data.categoryName").value("치킨"))
                    .andExpect(jsonPath("$.data.sortOrder").value(1));

            then(storeCategoryService).should().createCategory(any(StoreCategoryCreateRequest.class));
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게 카테고리를 생성할 수 없다")
        void createCategory_fail_whenCustomer() throws Exception {
            // given
            StoreCategoryCreateRequest request = new StoreCategoryCreateRequest(
                    "치킨",
                    "치킨 카테고리",
                    true
            );

            // when & then
            mockMvc.perform(post("/api/v1/store-categories")
                            .with(csrf())
                            .with(authentication(customerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(storeCategoryService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("가게 카테고리 조회 API")
    class GetCategoryApiTest {

        @Test
        @DisplayName("가게 카테고리 목록을 조회한다")
        void getCategories_success() throws Exception {
            // given
            StoreCategoryResponse response = new StoreCategoryResponse(
                    UUID.randomUUID(),
                    "치킨",
                    "치킨 카테고리",
                    1,
                    true
            );

            given(storeCategoryService.getCategories()).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/store-categories")
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].categoryName").value("치킨"))
                    .andExpect(jsonPath("$.data[0].sortOrder").value(1));

            then(storeCategoryService).should().getCategories();
        }

        @Test
        @DisplayName("활성 가게 카테고리 목록을 조회한다")
        void getActiveCategories_success() throws Exception {
            // given
            StoreCategoryResponse response = new StoreCategoryResponse(
                    UUID.randomUUID(),
                    "치킨",
                    "치킨 카테고리",
                    1,
                    true
            );

            given(storeCategoryService.getActiveCategories()).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/store-categories/active")
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].categoryName").value("치킨"));

            then(storeCategoryService).should().getActiveCategories();
        }

        @Test
        @DisplayName("가게 카테고리를 단건 조회한다")
        void getCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategoryResponse response = new StoreCategoryResponse(
                    categoryId,
                    "치킨",
                    "치킨 카테고리",
                    1,
                    true
            );

            given(storeCategoryService.getCategory(categoryId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/store-categories/{categoryId}", categoryId)
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categoryId").value(categoryId.toString()))
                    .andExpect(jsonPath("$.data.categoryName").value("치킨"));

            then(storeCategoryService).should().getCategory(categoryId);
        }
    }

    @Nested
    @DisplayName("가게 카테고리 수정 API")
    class UpdateCategoryApiTest {

        @Test
        @DisplayName("MANAGER 권한이면 가게 카테고리를 수정한다")
        void updateCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategoryUpdateRequest request = new StoreCategoryUpdateRequest(
                    "치킨 수정",
                    "설명 수정",
                    2,
                    false
            );
            StoreCategoryResponse response = new StoreCategoryResponse(
                    categoryId,
                    "치킨 수정",
                    "설명 수정",
                    2,
                    false
            );

            given(storeCategoryService.updateCategory(eq(categoryId), any(StoreCategoryUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/store-categories/{categoryId}", categoryId)
                            .with(csrf())
                            .with(authentication(managerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.categoryName").value("치킨 수정"))
                    .andExpect(jsonPath("$.data.sortOrder").value(2));

            then(storeCategoryService).should().updateCategory(eq(categoryId), any(StoreCategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게 카테고리를 수정할 수 없다")
        void updateCategory_fail_whenCustomer() throws Exception {
            // given
            StoreCategoryUpdateRequest request = new StoreCategoryUpdateRequest(
                    "치킨 수정",
                    "설명 수정",
                    2,
                    false
            );

            // when & then
            mockMvc.perform(put("/api/v1/store-categories/{categoryId}", UUID.randomUUID())
                            .with(csrf())
                            .with(authentication(customerAuthentication()))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            then(storeCategoryService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("가게 카테고리 삭제 API")
    class DeleteCategoryApiTest {

        @Test
        @DisplayName("MASTER 권한이면 가게 카테고리를 삭제한다")
        void deleteCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/v1/store-categories/{categoryId}", categoryId)
                            .with(csrf())
                            .with(authentication(masterAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(storeCategoryService).should().deleteCategory(categoryId, 1L);
        }

        @Test
        @DisplayName("CUSTOMER 권한이면 가게 카테고리를 삭제할 수 없다")
        void deleteCategory_fail_whenCustomer() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/store-categories/{categoryId}", UUID.randomUUID())
                            .with(csrf())
                            .with(authentication(customerAuthentication())))
                    .andExpect(status().isForbidden());

            then(storeCategoryService).shouldHaveNoInteractions();
        }
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
