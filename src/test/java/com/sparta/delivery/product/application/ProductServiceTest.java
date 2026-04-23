package com.sparta.delivery.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("상품 생성")
    class Create {

        @Test
        @DisplayName("OWNER가 본인 가게에 상품을 생성한다")
        void create_success_owner() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, "coffee", 1);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));
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
        @DisplayName("권한이 없으면 상품을 생성할 수 없다")
        void create_fail_forbidden() {
            // given
            Long actorId = 2L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, 1L);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, null, 1);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> productService.create(actorId, UserRole.OWNER, storeId, request))
                    .isInstanceOf(ProductForbiddenException.class);
            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("같은 가게에 동일한 상품명이 있으면 생성할 수 없다")
        void create_fail_duplicateName() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            Store store = createStore(storeId, ownerId);
            ProductCreateRequest request = new ProductCreateRequest("Americano", 4500, null, 1);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Americano")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.create(ownerId, UserRole.OWNER, storeId, request))
                    .isInstanceOf(DuplicateProductNameException.class);
            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("상품 조회")
    class Read {

        @Test
        @DisplayName("숨김 상품이 아니면 비로그인 사용자도 단건 조회할 수 있다")
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
        @DisplayName("비로그인 사용자는 숨김 상품을 단건 조회할 수 없다")
        void getProduct_fail_anonymous_whenHidden() {
            // given
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            product.changeHidden(true);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));

            // when & then
            assertThatThrownBy(() -> productService.getProduct(null, null, productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("OWNER는 본인 가게의 숨김 상품을 단건 조회할 수 있다")
        void getProduct_success_owner_whenHiddenAndOwned() {
            // given
            Long ownerId = 1L;
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            product.changeHidden(true);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.getProduct(ownerId, UserRole.OWNER, productId);

            // then
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.isHidden()).isTrue();
        }

        @Test
        @DisplayName("비로그인 사용자는 목록 조회 시 숨김 상품을 제외한다")
        void getProducts_success_anonymous_excludesHidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Product visibleProduct = createProduct(UUID.randomUUID(), storeId, "Americano");

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findAllByStoreIdAndIsHiddenFalseOrderByDisplayOrderAsc(storeId))
                    .willReturn(List.of(visibleProduct));

            // when
            var responses = productService.getProducts(null, null, storeId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).productName()).isEqualTo("Americano");
            then(productRepository).should(never()).findAllByStoreIdOrderByDisplayOrderAsc(storeId);
        }

        @Test
        @DisplayName("MANAGER는 목록 조회 시 숨김 상품을 포함한다")
        void getProducts_success_manager_includesHidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Product hiddenProduct = createProduct(UUID.randomUUID(), storeId, "Latte");
            hiddenProduct.changeHidden(true);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, 1L)));
            given(productRepository.findAllByStoreIdOrderByDisplayOrderAsc(storeId)).willReturn(List.of(hiddenProduct));

            // when
            var responses = productService.getProducts(99L, UserRole.MANAGER, storeId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).isHidden()).isTrue();
            then(productRepository).should(never()).findAllByStoreIdAndIsHiddenFalseOrderByDisplayOrderAsc(storeId);
        }
    }

    @Nested
    @DisplayName("상품 수정")
    class Update {

        @Test
        @DisplayName("상품 일반 정보를 수정한다")
        void update_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, "milk coffee", 2);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Latte")).willReturn(false);

            // when
            var response = productService.update(ownerId, UserRole.OWNER, productId, request);

            // then
            assertThat(response.productName()).isEqualTo("Latte");
            assertThat(response.price()).isEqualTo(5500);
            assertThat(response.descriptionSource()).isEqualTo(DescriptionSource.MANUAL);
        }

        @Test
        @DisplayName("상품명 변경 시 중복되면 수정할 수 없다")
        void update_fail_duplicateName_whenNameChanged() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");
            ProductUpdateRequest request = new ProductUpdateRequest("Latte", 5500, null, 2);

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));
            given(productRepository.existsByStoreIdAndProductName(storeId, "Latte")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.update(ownerId, UserRole.OWNER, productId, request))
                    .isInstanceOf(DuplicateProductNameException.class);
        }
    }

    @Nested
    @DisplayName("상품 상태 변경 및 삭제")
    class StatusAndDelete {

        @Test
        @DisplayName("상품 숨김 상태를 변경한다")
        void changeHidden_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.changeHidden(ownerId, UserRole.OWNER, productId, new ProductHiddenUpdateRequest(true));

            // then
            assertThat(response.isHidden()).isTrue();
        }

        @Test
        @DisplayName("상품 품절 상태를 변경한다")
        void changeSoldOut_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

            // when
            var response = productService.changeSoldOut(ownerId, UserRole.OWNER, productId, new ProductSoldOutUpdateRequest(true));

            // then
            assertThat(response.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("상품을 soft delete 처리한다")
        void delete_success() {
            // given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = createProduct(productId, storeId, "Americano");

            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(createStore(storeId, ownerId)));

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
