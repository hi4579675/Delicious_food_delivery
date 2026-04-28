package com.sparta.delivery.product.presentation;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.sparta.delivery.auth.infrastructure.jwt.JwtAuthenticationFilter;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.product.application.ProductService;
import com.sparta.delivery.product.domain.entity.DescriptionSource;
import com.sparta.delivery.product.presentation.dto.request.ProductCreateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductHiddenUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductSoldOutUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductUpdateRequest;
import com.sparta.delivery.product.presentation.dto.response.ProductResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.sparta.delivery.common.config.security.SecurityConfig;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Nested
    @DisplayName("Create product API")
    class CreateProductApi {

        @Test
        @DisplayName("creates a product for OWNER role")
        void createProduct_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, "coffee", 1, false, null);
            ProductResponse response = productResponse(UUID.randomUUID(), storeId, "Americano", false, false);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class)))
                    .willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(post("/api/v1/stores/{storeId}/products", storeId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(201))
                        .andExpect(jsonPath("$.data.productName").value("Americano"));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class));
        }

        @Test
        @DisplayName("creates a product with AI generated description for OWNER role")
        void createProduct_success_aiGeneratedDescription() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest(
                    "Americano",
                    4500,
                    null,
                    1,
                    true,
                    "고소한 맛을 강조해줘"
            );
            ProductResponse response = new ProductResponse(
                    productId,
                    storeId,
                    "Americano",
                    "AI generated description",
                    DescriptionSource.AI_GENERATED,
                    4500,
                    false,
                    false,
                    1,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class)))
                    .willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(post("/api/v1/stores/{storeId}/products", storeId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(201))
                        .andExpect(jsonPath("$.data.productName").value("Americano"))
                        .andExpect(jsonPath("$.data.description").value("AI generated description"))
                        .andExpect(jsonPath("$.data.descriptionSource").value("AI_GENERATED"));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class));
        }

        @Test
        @DisplayName("returns 400 when product name is blank")
        void createProduct_fail_whenProductNameBlank() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest("", 4500, null, 1, false, null);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(post("/api/v1/stores/{storeId}/products", storeId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Read product API")
    class ReadProductApi {

        @Test
        @DisplayName("reads a single product for anonymous user")
        void getProduct_success_anonymous() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", false, false);

            given(productService.getProduct(null, null, productId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/products/{productId}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                    .andExpect(jsonPath("$.data.productName").value("Americano"));

            then(productService).should().getProduct(null, null, productId);
        }

        @Test
        @DisplayName("reads paged product list for anonymous user")
        void getProducts_success_anonymous() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductResponse response = productResponse(UUID.randomUUID(), storeId, "Americano", false, false);

            given(productService.getProducts(null, null, storeId, 0, 10, null, null))
                    .willReturn(new PageImpl<>(List.of(response)));

            // when & then
            mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.content[0].productName").value("Americano"));

            then(productService).should().getProducts(null, null, storeId, 0, 10, null, null);
        }

        @Test
        @DisplayName("reads searched product list for anonymous user")
        void getProducts_search_success_anonymous() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductResponse response = productResponse(UUID.randomUUID(), storeId, "Americano", false, false);

            given(productService.getProducts(null, null, storeId, 0, 10, null, "Ameri"))
                    .willReturn(new PageImpl<>(List.of(response)));

            // when & then
            mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId)
                            .param("page", "0")
                            .param("size", "10")
                            .param("keyword", "Ameri"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.content[0].productName").value("Americano"));

            then(productService).should().getProducts(null, null, storeId, 0, 10, null, "Ameri");
        }
    }

    @Nested
    @DisplayName("Update product API")
    class UpdateProductApi {

        @Test
        @DisplayName("updates product info for OWNER role")
        void updateProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, "milk coffee", 2);
            ProductResponse response = productResponse(productId, storeId, "Latte", false, false);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.update(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(put("/api/v1/products/{productId}", productId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.productName").value("Latte"));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().update(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("Product status and delete API")
    class StatusAndDeleteApi {

        @Test
        @DisplayName("changes hidden status for OWNER role")
        void changeHidden_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductHiddenUpdateRequest request = new ProductHiddenUpdateRequest(true);
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", true, false);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.changeHidden(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductHiddenUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(patch("/api/v1/products/{productId}/hidden", productId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isHidden").value(true));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().changeHidden(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductHiddenUpdateRequest.class));
        }

        @Test
        @DisplayName("changes sold-out status for OWNER role")
        void changeSoldOut_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductSoldOutUpdateRequest request = new ProductSoldOutUpdateRequest(true);
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", false, true);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.changeSoldOut(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductSoldOutUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(patch("/api/v1/products/{productId}/sold-out", productId)
                                .with(csrf())
                                .with(authentication(auth))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isSoldOut").value(true));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().changeSoldOut(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductSoldOutUpdateRequest.class));
        }

        @Test
        @DisplayName("deletes a product for OWNER role")
        void deleteProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", false, false);
            UsernamePasswordAuthenticationToken auth = authenticationToken(UserRole.OWNER);

            given(productService.delete(1L, UserRole.OWNER, productId)).willReturn(response);

            // when & then
            TestSecurityContextHolder.setAuthentication(auth);
            try {
                mockMvc.perform(delete("/api/v1/products/{productId}", productId)
                                .with(csrf())
                                .with(authentication(auth)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.productId").value(productId.toString()));
            } finally {
                TestSecurityContextHolder.clearContext();
            }

            then(productService).should().delete(1L, UserRole.OWNER, productId);
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

    private ProductResponse productResponse(
            UUID productId,
            UUID storeId,
            String productName,
            boolean hidden,
            boolean soldOut
    ) {
        return new ProductResponse(
                productId,
                storeId,
                productName,
                "description",
                DescriptionSource.MANUAL,
                4500,
                soldOut,
                hidden,
                1,
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
