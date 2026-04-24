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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.sparta.delivery.common.config.security.SecurityConfig;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Nested
    @DisplayName("상품 생성 API")
    class CreateProductApi {

        @Test
        @DisplayName("OWNER 권한이면 상품을 생성한다")
        void createProduct_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, "coffee", 1);
            ProductResponse response = productResponse(UUID.randomUUID(), storeId, "Americano", false, false);

            given(productService.create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/stores/{storeId}/products", storeId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.productName").value("Americano"));

            then(productService).should().create(eq(1L), eq(UserRole.OWNER), eq(storeId), any(ProductCreateRequest.class));
        }

        @Test
        @DisplayName("상품명이 비어있으면 400을 반환한다")
        void createProduct_fail_whenProductNameBlank() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest("", 4500, null, 1);

            // when & then
            mockMvc.perform(post("/api/v1/stores/{storeId}/products", storeId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(productService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 조회 API")
    class ReadProductApi {

        @Test
        @DisplayName("비로그인 사용자가 상품을 단건 조회한다")
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
        @DisplayName("비로그인 사용자가 가게 상품 목록을 조회한다")
        void getProducts_success_anonymous() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            ProductResponse response = productResponse(UUID.randomUUID(), storeId, "Americano", false, false);

            given(productService.getProducts(null, null, storeId)).willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data[0].productName").value("Americano"));

            then(productService).should().getProducts(null, null, storeId);
        }
    }

    @Nested
    @DisplayName("상품 수정 API")
    class UpdateProductApi {

        @Test
        @DisplayName("OWNER 권한이면 상품 일반 정보를 수정한다")
        void updateProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, "milk coffee", 2);
            ProductResponse response = productResponse(productId, storeId, "Latte", false, false);

            given(productService.update(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/products/{productId}", productId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productName").value("Latte"));

            then(productService).should().update(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("상품 상태 변경 및 삭제 API")
    class StatusAndDeleteApi {

        @Test
        @DisplayName("OWNER 권한이면 상품 숨김 상태를 변경한다")
        void changeHidden_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductHiddenUpdateRequest request = new ProductHiddenUpdateRequest(true);
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", true, false);

            given(productService.changeHidden(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductHiddenUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/products/{productId}/hidden", productId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isHidden").value(true));

            then(productService).should().changeHidden(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductHiddenUpdateRequest.class));
        }

        @Test
        @DisplayName("OWNER 권한이면 상품 품절 상태를 변경한다")
        void changeSoldOut_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductSoldOutUpdateRequest request = new ProductSoldOutUpdateRequest(true);
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", false, true);

            given(productService.changeSoldOut(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductSoldOutUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/products/{productId}/sold-out", productId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER)))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isSoldOut").value(true));

            then(productService).should().changeSoldOut(eq(1L), eq(UserRole.OWNER), eq(productId), any(ProductSoldOutUpdateRequest.class));
        }

        @Test
        @DisplayName("OWNER 권한이면 상품을 삭제한다")
        void deleteProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            ProductResponse response = productResponse(productId, UUID.randomUUID(), "Americano", false, false);

            given(productService.delete(1L, UserRole.OWNER, productId)).willReturn(response);

            // when & then
            mockMvc.perform(delete("/api/v1/products/{productId}", productId)
                            .with(csrf())
                            .with(authentication(authenticationToken(UserRole.OWNER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(productId.toString()));

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
