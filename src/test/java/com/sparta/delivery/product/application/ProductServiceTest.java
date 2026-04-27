package com.sparta.delivery.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sparta.delivery.ai.application.AiDescriptionService;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.product.domain.entity.DescriptionSource;
import com.sparta.delivery.product.domain.entity.Product;
import com.sparta.delivery.product.domain.exception.DuplicateProductNameException;
import com.sparta.delivery.product.domain.exception.ProductForbiddenException;
import com.sparta.delivery.product.domain.exception.ProductNotFoundException;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import com.sparta.delivery.product.presentation.dto.request.ProductCreateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductHiddenUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductSoldOutUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductUpdateRequest;
import com.sparta.delivery.product.presentation.dto.response.ProductResponse;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.math.BigDecimal;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AiDescriptionService aiDescriptionService;

    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("Create product")
    class Create {

        @Test
        @DisplayName("creates a product for owned store")
        void create_success_owner() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, "coffee", 1, false, null);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Americano")).willReturn(false);
            given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                ReflectionTestUtils.setField(product, "productId", UUID.randomUUID());
                return product;
            });

            // when
            var response = productService.create(ownerId, UserRole.OWNER, storeId, request);

            // then
            assertThat(response.storeId()).isEqualTo(storeId);
            assertThat(response.productName()).isEqualTo("Americano");
            assertThat(response.descriptionSource()).isEqualTo(DescriptionSource.MANUAL);
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        @DisplayName("fails when actor has no permission")
        void create_fail_forbidden() {
            // given
            Long actorId = 2L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, 1L);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, null, 1, false, null);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> productService.create(actorId, UserRole.OWNER, storeId, request))
                    .isInstanceOf(ProductForbiddenException.class);
            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("fails when product name is duplicated in store")
        void create_fail_duplicateName() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, null, 1, false, null);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Americano")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.create(ownerId, UserRole.OWNER, storeId, request))
                    .isInstanceOf(DuplicateProductNameException.class);
            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("creates a product with AI generated description")
        void create_success_aiGeneratedDescription() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest(
                    "Americano",
                    4500,
                    null,
                    1,
                    true,
                    "고소한 맛을 강조해줘"
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Americano")).willReturn(false);
            given(aiDescriptionService.generateDescription(
                    ownerId,
                    "Americano",
                    4500,
                    "고소한 맛을 강조해줘"
            )).willReturn("AI generated description");
            given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                ReflectionTestUtils.setField(product, "productId", UUID.randomUUID());
                return product;
            });

            // when
            var response = productService.create(ownerId, UserRole.OWNER, storeId, request);

            // then
            assertThat(response.description()).isEqualTo("AI generated description");
            assertThat(response.descriptionSource()).isEqualTo(DescriptionSource.AI_GENERATED);
            then(aiDescriptionService).should().generateDescription(
                    ownerId,
                    "Americano",
                    4500,
                    "고소한 맛을 강조해줘"
            );
        }

        @Test
        @DisplayName("fails to create product when AI description generation fails")
        void create_fail_whenAiDescriptionGenerationFails() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest(
                    "Americano",
                    4500,
                    null,
                    1,
                    true,
                    "고소한 맛을 강조해줘"
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Americano")).willReturn(false);
            given(aiDescriptionService.generateDescription(
                    ownerId,
                    "Americano",
                    4500,
                    "고소한 맛을 강조해줘"
            )).willThrow(new ExternalLlmCallFailedException());

            // when & then
            assertThatThrownBy(() -> productService.create(ownerId, UserRole.OWNER, storeId, request))
                    .isInstanceOf(ExternalLlmCallFailedException.class);
            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Read product")
    class Read {

        @Test
        @DisplayName("reads visible product for anonymous user")
        void getProduct_success_anonymous_whenVisible() {
            // given
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, UUID.randomUUID(), "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));

            // when
            var response = productService.getProduct(null, null, productId);

            // then
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.productName()).isEqualTo("Americano");
            then(storeRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fails to read hidden product for anonymous user")
        void getProduct_fail_anonymous_whenHidden() {
            // given
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            product.changeHidden(true);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));

            // when & then
            assertThatThrownBy(() -> productService.getProduct(null, null, productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("reads hidden product for store owner")
        void getProduct_success_owner_whenHiddenAndOwned() {
            // given
            Long ownerId = 1L;
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            product.changeHidden(true);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.getProduct(ownerId, UserRole.OWNER, productId);

            // then
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.isHidden()).isTrue();
        }

        @Test
        @DisplayName("excludes hidden products for anonymous list read")
        void getProducts_success_anonymous_excludesHidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Product visibleProduct = createProduct(UUID.randomUUID(), storeId, "Americano");

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreIdAndIsHiddenFalse(eq(storeId), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(visibleProduct)));

            // when
            Page<ProductResponse> responses = productService.getProducts(null, null, storeId, 0, 10, null, null);

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).productName()).isEqualTo("Americano");
            then(productRepository).should(never()).findByStoreId(eq(storeId), any(Pageable.class));
        }

        @Test
        @DisplayName("includes hidden products for manager list read")
        void getProducts_success_manager_includesHidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Product hiddenProduct = createProduct(UUID.randomUUID(), storeId, "Latte");
            hiddenProduct.changeHidden(true);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreId(eq(storeId), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(hiddenProduct)));

            // when
            Page<ProductResponse> responses = productService.getProducts(99L, UserRole.MANAGER, storeId, 0, 10, null, null);

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).isHidden()).isTrue();
            then(productRepository).should(never()).findByStoreIdAndIsHiddenFalse(eq(storeId), any(Pageable.class));
        }

        @Test
        @DisplayName("normalizes invalid page size and applies default sort")
        void getProducts_normalizesPageable() {
            // given
            UUID storeId = UUID.randomUUID();

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreId(eq(storeId), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            productService.getProducts(99L, UserRole.MANAGER, storeId, 0, 999, null, null);

            // then
            then(productRepository).should().findByStoreId(eq(storeId), argThat(pageable ->
                    pageable.getPageNumber() == 0
                            && pageable.getPageSize() == 10
                            && pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "createdAt"))
            ));
        }

        @Test
        @DisplayName("searches visible products by keyword for anonymous user")
        void getProducts_search_success_anonymous() {
            // given
            UUID storeId = UUID.randomUUID();
            Product visibleProduct = createProduct(UUID.randomUUID(), storeId, "Americano");

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreIdAndIsHiddenFalseAndProductNameContainingIgnoreCase(
                    eq(storeId), eq("Ameri"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(visibleProduct)));

            // when
            Page<ProductResponse> responses = productService.getProducts(null, null, storeId, 0, 10, null, "Ameri");

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).productName()).isEqualTo("Americano");
            then(productRepository).should(never())
                    .findByStoreIdAndProductNameContainingIgnoreCase(eq(storeId), eq("Ameri"), any(Pageable.class));
        }

        @Test
        @DisplayName("searches all products by keyword for manager")
        void getProducts_search_success_manager() {
            // given
            UUID storeId = UUID.randomUUID();
            Product hiddenProduct = createProduct(UUID.randomUUID(), storeId, "Latte");
            hiddenProduct.changeHidden(true);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreIdAndProductNameContainingIgnoreCase(
                    eq(storeId), eq("Lat"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(hiddenProduct)));

            // when
            Page<ProductResponse> responses = productService.getProducts(99L, UserRole.MANAGER, storeId, 0, 10, null, "Lat");

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).isHidden()).isTrue();
            then(productRepository).should(never())
                    .findByStoreIdAndIsHiddenFalseAndProductNameContainingIgnoreCase(eq(storeId), eq("Lat"), any(Pageable.class));
        }

        @Test
        @DisplayName("normalizes blank keyword to default list read")
        void getProducts_normalizesBlankKeyword() {
            // given
            UUID storeId = UUID.randomUUID();

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findByStoreId(eq(storeId), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            productService.getProducts(99L, UserRole.MANAGER, storeId, 0, 10, null, "   ");

            // then
            then(productRepository).should().findByStoreId(eq(storeId), any(Pageable.class));
            then(productRepository).should(never())
                    .findByStoreIdAndProductNameContainingIgnoreCase(eq(storeId), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Update product")
    class Update {

        @Test
        @DisplayName("updates product info")
        void update_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, "milk coffee", 2);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Latte")).willReturn(false);

            // when
            var response = productService.update(ownerId, UserRole.OWNER, productId, request);

            // then
            assertThat(response.productName()).isEqualTo("Latte");
            assertThat(response.price()).isEqualTo(5500);
            assertThat(response.descriptionSource()).isEqualTo(DescriptionSource.MANUAL);
        }

        @Test
        @DisplayName("fails update when changed name is duplicated")
        void update_fail_duplicateName_whenNameChanged() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, null, 2);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Latte")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.update(ownerId, UserRole.OWNER, productId, request))
                    .isInstanceOf(DuplicateProductNameException.class);
        }
    }

    @Nested
    @DisplayName("Product status and delete")
    class StatusAndDelete {

        @Test
        @DisplayName("changes hidden status")
        void changeHidden_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.changeHidden(ownerId, UserRole.OWNER, productId, new ProductHiddenUpdateRequest(true));

            // then
            assertThat(response.isHidden()).isTrue();
        }

        @Test
        @DisplayName("changes sold-out status")
        void changeSoldOut_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.changeSoldOut(ownerId, UserRole.OWNER, productId, new ProductSoldOutUpdateRequest(true));

            // then
            assertThat(response.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("soft deletes product")
        void delete_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            productService.delete(ownerId, UserRole.OWNER, productId);

            // then
            assertThat(product.isDeleted()).isTrue();
            assertThat(product.getDeletedBy()).isEqualTo(ownerId);
        }
    }

    private Store createStore(UUID storeId, Long ownerId) {
        Store store = Store.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ownerId,
                "store",
                "description",
                "address",
                "detail",
                "01012345678",
                10000,
                true,
                true,
                BigDecimal.ZERO,
                0
        );
        ReflectionTestUtils.setField(store, "storeId", storeId);
        return store;
    }

    private Product createProduct(UUID productId, UUID storeId, String productName) {
        Product product = Product.create(
                storeId,
                productName,
                null,
                null,
                4500,
                1
        );
        ReflectionTestUtils.setField(product, "productId", productId);
        return product;
    }
}
