package com.sparta.delivery.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sparta.delivery.address.application.AddressService;
import com.sparta.delivery.address.domain.entity.Address;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderItem;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.domain.exception.InvalidOrderStatusException;
import com.sparta.delivery.order.domain.exception.MinOrderAmountNotMetException;
import com.sparta.delivery.order.domain.exception.OrderForbiddenException;
import com.sparta.delivery.order.domain.exception.ProductNotOrderableException;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.order.presentation.dto.OrderCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderItemCreateRequest;
import com.sparta.delivery.product.domain.entity.Product;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 생성")
    class Create {

        @Test
        @DisplayName("본인 주소와 주문 가능한 상품으로 주문을 생성할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Address address = createAddress(userId, addressId);
            Store store = createStore(storeId, 10000, true, true);
            Product product = createProduct(productId, storeId, "후라이드 치킨", 18000);
            OrderCreateRequest request = new OrderCreateRequest(
                    storeId,
                    addressId,
                    List.of(new OrderItemCreateRequest(productId, 1))
            );

            given(addressService.findOwnedAddress(userId, addressId)).willReturn(address);
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));
            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                ReflectionTestUtils.setField(order, "orderId", UUID.randomUUID());
                ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
                return order;
            });

            // when
            var response = orderService.create(userId, request);

            // then
            assertThat(response.storeId()).isEqualTo(storeId);
            assertThat(response.addressId()).isEqualTo(addressId);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.totalPrice()).isEqualTo(18000);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.deliveryAddressSnapshot()).isEqualTo("[12345] 서울시 강남구 101호");
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("최소 주문 금액을 충족하지 않으면 주문을 생성할 수 없다")
        void minOrderAmountNotMet() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Address address = createAddress(userId, addressId);
            Store store = createStore(storeId, 20000, true, true);
            Product product = createProduct(productId, storeId, "후라이드 치킨", 18000);
            OrderCreateRequest request = new OrderCreateRequest(
                    storeId,
                    addressId,
                    List.of(new OrderItemCreateRequest(productId, 1))
            );

            given(addressService.findOwnedAddress(userId, addressId)).willReturn(address);
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));
            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));

            // when // then
            assertThatThrownBy(() -> orderService.create(userId, request))
                    .isInstanceOf(MinOrderAmountNotMetException.class);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("주문 불가능한 상품이면 주문을 생성할 수 없다")
        void productNotOrderable() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Address address = createAddress(userId, addressId);
            Store store = createStore(storeId, 10000, true, true);
            Product product = createProduct(productId, storeId, "후라이드 치킨", 18000);
            product.changeHidden(true);
            OrderCreateRequest request = new OrderCreateRequest(
                    storeId,
                    addressId,
                    List.of(new OrderItemCreateRequest(productId, 1))
            );

            given(addressService.findOwnedAddress(userId, addressId)).willReturn(address);
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));
            given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));

            // when // then
            assertThatThrownBy(() -> orderService.create(userId, request))
                    .isInstanceOf(ProductNotOrderableException.class);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrders {

        @Test
        @DisplayName("허용되지 않은 페이지 크기는 10으로 보정된다")
        void invalidPageSize() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, userId, storeId, UUID.randomUUID(), 18000);
            Pageable pageable = PageRequest.of(0, 7);

            given(orderRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));

            // when
            PageResponse<?> response = orderService.getOrders(userId, UserRole.CUSTOMER, null, null, pageable);

            // then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(orderRepository).findAllByUserId(eq(userId), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
            assertThat(response.content()).hasSize(1);
            assertThat(response.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("사장은 본인 가게 주문 목록을 조회할 수 있다")
        void ownerSuccess() {
            // given
            Long actorId = 1L;
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(UUID.randomUUID(), 2L, storeId, UUID.randomUUID(), 18000);
            Pageable pageable = PageRequest.of(0, 10);

            given(storeRepository.findByUserIdAndDeletedAtIsNull(actorId))
                    .willReturn(List.of(createStore(storeId, actorId, 10000, true, true)));
            given(orderRepository.findAllByStoreIdIn(eq(List.of(storeId)), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(order), pageable, 1));

            // when
            PageResponse<?> response = orderService.getOrders(actorId, UserRole.OWNER, null, null, pageable);

            // then
            assertThat(response.content()).hasSize(1);
        }

        @Test
        @DisplayName("매니저는 전체 주문 목록을 조회할 수 있다")
        void managerSuccess() {
            // given
            Long actorId = 99L;
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(UUID.randomUUID(), 2L, storeId, UUID.randomUUID(), 18000);
            Pageable pageable = PageRequest.of(0, 10);

            given(orderRepository.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(order), pageable, 1));

            // when
            PageResponse<?> response = orderService.getOrders(actorId, UserRole.MANAGER, null, null, pageable);

            // then
            assertThat(response.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("주문 상세 조회")
    class GetOrder {

        @Test
        @DisplayName("본인 주문을 상세 조회할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            Order order = createOrder(orderId, userId, storeId, addressId, 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            var response = orderService.getOrder(userId, UserRole.CUSTOMER, orderId);

            // then
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.totalPrice()).isEqualTo(18000);
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("본인 주문이 아니면 상세 조회할 수 없다")
        void forbidden() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, UUID.randomUUID(), UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when // then
            assertThatThrownBy(() -> orderService.getOrder(userId, UserRole.CUSTOMER, orderId))
                    .isInstanceOf(OrderForbiddenException.class);
        }

        @Test
        @DisplayName("사장은 본인 가게 주문을 상세 조회할 수 있다")
        void ownerSuccess() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, storeId, UUID.randomUUID(), 18000);
            Store store = createStore(storeId, actorId, 10000, true, true);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            // when
            var response = orderService.getOrder(actorId, UserRole.OWNER, orderId);

            // then
            assertThat(response.orderId()).isEqualTo(orderId);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class Cancel {

        @Test
        @DisplayName("본인 주문을 취소할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, userId, UUID.randomUUID(), UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            orderService.cancel(userId, UserRole.CUSTOMER, orderId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(order.getCanceledAt()).isNotNull();
        }

        @Test
        @DisplayName("본인 주문이 아니면 취소할 수 없다")
        void forbidden() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, UUID.randomUUID(), UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when // then
            assertThatThrownBy(() -> orderService.cancel(userId, UserRole.CUSTOMER, orderId))
                    .isInstanceOf(OrderForbiddenException.class);
        }

        @Test
        @DisplayName("마스터는 타인 주문도 취소할 수 있다")
        void masterSuccess() {
            // given
            Long actorId = 99L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, UUID.randomUUID(), UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            orderService.cancel(actorId, UserRole.MASTER, orderId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(order.getCanceledAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("주문 상태 변경")
    class UpdateStatus {

        @Test
        @DisplayName("본인 가게 사장은 주문 상태를 변경할 수 있다")
        void ownerSuccess() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, storeId, UUID.randomUUID(), 18000);
            Store store = createStore(storeId, actorId, 10000, true, true);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            // when
            var response = orderService.updateStatus(actorId, UserRole.OWNER, orderId, OrderStatus.ACCEPTED);

            // then
            assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("매니저는 주문 상태를 변경할 수 있다")
        void managerSuccess() {
            // given
            Long actorId = 99L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, storeId, UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            var response = orderService.updateStatus(actorId, UserRole.MANAGER, orderId, OrderStatus.ACCEPTED);

            // then
            assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(storeRepository, never()).findByStoreIdAndDeletedAtIsNull(any(UUID.class));
        }

        @Test
        @DisplayName("본인 가게 주문이 아니면 상태를 변경할 수 없다")
        void forbidden() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, storeId, UUID.randomUUID(), 18000);
            Store store = createStore(storeId, 3L, 10000, true, true);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            // when // then
            assertThatThrownBy(() -> orderService.updateStatus(actorId, UserRole.OWNER, orderId, OrderStatus.ACCEPTED))
                    .isInstanceOf(OrderForbiddenException.class);
        }

        @Test
        @DisplayName("지원하지 않는 주문 상태로는 변경할 수 없다")
        void invalidStatus() {
            // given
            Long actorId = 99L;
            UUID orderId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, storeId, UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when // then
            assertThatThrownBy(() -> orderService.updateStatus(actorId, UserRole.MANAGER, orderId, OrderStatus.PENDING))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }
    }

    @Nested
    @DisplayName("주문 삭제")
    class Delete {

        @Test
        @DisplayName("마스터는 주문을 삭제할 수 있다")
        void success() {
            // given
            Long actorId = 99L;
            UUID orderId = UUID.randomUUID();
            Order order = createOrder(orderId, 2L, UUID.randomUUID(), UUID.randomUUID(), 18000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            orderService.delete(actorId, UserRole.MASTER, orderId);

            // then
            assertThat(order.getDeletedAt()).isNotNull();
            assertThat(order.getDeletedBy()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("마스터가 아니면 주문을 삭제할 수 없다")
        void forbidden() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();

            // when // then
            assertThatThrownBy(() -> orderService.delete(actorId, UserRole.CUSTOMER, orderId))
                    .isInstanceOf(OrderForbiddenException.class);
            verify(orderRepository, never()).findById(any(UUID.class));
        }
    }

    private Address createAddress(Long userId, UUID addressId) {
        Address address = Address.create(userId, "집", "서울시 강남구", "101호", "12345", true);
        ReflectionTestUtils.setField(address, "addressId", addressId);
        return address;
    }

    private Store createStore(UUID storeId, Integer minOrderAmount, boolean isOpen, boolean isActive) {
        return createStore(storeId, 1L, minOrderAmount, isOpen, isActive);
    }

    private Store createStore(UUID storeId, Long ownerId, Integer minOrderAmount, boolean isOpen, boolean isActive) {
        Store store = Store.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ownerId,
                "치킨집",
                null,
                "서울시 강남구",
                null,
                null,
                minOrderAmount,
                isOpen,
                isActive,
                null,
                0
        );
        ReflectionTestUtils.setField(store, "storeId", storeId);
        return store;
    }

    private Product createProduct(UUID productId, UUID storeId, String productName, Integer price) {
        Product product = Product.create(
                storeId,
                productName,
                null,
                null,
                price,
                1
        );
        ReflectionTestUtils.setField(product, "productId", productId);
        return product;
    }

    private Order createOrder(UUID orderId, Long userId, UUID storeId, UUID addressId, Integer unitPrice) {
        OrderItem orderItem = OrderItem.create(UUID.randomUUID(), 1, unitPrice, "후라이드 치킨");
        Order order = Order.create(
                storeId,
                addressId,
                userId,
                "[12345] 서울시 강남구 101호",
                List.of(orderItem)
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        return order;
    }
}
