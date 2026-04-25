package com.sparta.delivery.order.application;

import com.sparta.delivery.address.application.AddressService;
import com.sparta.delivery.address.domain.entity.Address;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderItem;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.domain.exception.MinOrderAmountNotMetException;
import com.sparta.delivery.order.domain.exception.OrderForbiddenException;
import com.sparta.delivery.order.domain.exception.OrderNotFoundException;
import com.sparta.delivery.order.domain.exception.ProductNotFoundException;
import com.sparta.delivery.order.domain.exception.ProductNotOrderableException;
import com.sparta.delivery.order.domain.exception.StoreNotFoundException;
import com.sparta.delivery.order.domain.exception.StoreNotOrderableException;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.order.presentation.dto.OrderCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderItemCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderDetailResponse;
import com.sparta.delivery.order.presentation.dto.OrderListResponse;
import com.sparta.delivery.product.domain.entity.Product;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);

    private final OrderRepository orderRepository;
    private final AddressService addressService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderDetailResponse create(Long userId, OrderCreateRequest request) {
        Address address = addressService.findOwnedAddress(userId, request.addressId());
        Store store = getOrderableStore(request.storeId());
        Order order = createOrder(userId, request, address, store);
        Order savedOrder = orderRepository.save(order);
        log.info("주문 생성 완료 - userId={}, orderId={}, totalPrice={}",
                userId, savedOrder.getOrderId(), savedOrder.getTotalPrice());
        return OrderDetailResponse.from(savedOrder);
    }

    public PageResponse<OrderListResponse> getMyOrders(
            Long userId,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        Page<OrderListResponse> page = findMyOrders(userId, storeId, status, pageable)
                .map(OrderListResponse::from);
        return PageResponse.from(page);
    }

    public OrderDetailResponse getMyOrder(Long userId, UUID orderId) {
        return OrderDetailResponse.from(getOwnedOrder(userId, orderId));
    }

    @Transactional
    public void cancel(Long userId, UUID orderId) {
        Order order = getOwnedOrder(userId, orderId);
        order.cancel();
        log.info("주문 취소 완료 - userId={}, orderId={}", userId, orderId);
    }

    private Order createOrder(Long userId, OrderCreateRequest request, Address address, Store store) {
        // 주문 총액은 외부 입력값이 아니라 주문 항목 합계로 계산되도록 엔티티에 맡긴다.
        List<OrderItem> items = createOrderItems(store.getStoreId(), request.items());
        Order order = Order.create(
                store.getStoreId(),
                address.getAddressId(),
                userId,
                buildDeliveryAddressSnapshot(address),
                items
        );
        validateMinOrderAmount(order, store);
        return order;
    }

    private Page<Order> findMyOrders(
            Long userId,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        Pageable normalizedPageable = normalizePageable(pageable);
        if (storeId != null && status != null) {
            return orderRepository.findAllByUserIdAndStoreIdAndStatus(
                    userId,
                    storeId,
                    status,
                    normalizedPageable
            );
        }
        if (storeId != null) {
            return orderRepository.findAllByUserIdAndStoreId(userId, storeId, normalizedPageable);
        }
        if (status != null) {
            return orderRepository.findAllByUserIdAndStatus(userId, status, normalizedPageable);
        }
        return orderRepository.findAllByUserId(userId, normalizedPageable);
    }

    private Pageable normalizePageable(Pageable pageable) {
        // size(10/30/50)와 기본 정렬(createdAt DESC)을 보정한다.
        int size = ALLOWED_PAGE_SIZES.contains(pageable.getPageSize()) ? pageable.getPageSize() : 10;
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private Store getOrderableStore(UUID storeId) {
        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(StoreNotFoundException::new);
        if (!Boolean.TRUE.equals(store.getIsOpen()) || !Boolean.TRUE.equals(store.getIsActive())) {
            throw new StoreNotOrderableException();
        }
        return store;
    }

    private List<OrderItem> createOrderItems(UUID storeId, List<OrderItemCreateRequest> requests) {
        return requests.stream()
                .map(request -> createOrderItem(storeId, request))
                .toList();
    }

    private OrderItem createOrderItem(UUID storeId, OrderItemCreateRequest request) {
        Product product = productRepository.findByProductId(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        validateOrderableProduct(storeId, product);

        return OrderItem.create(
                product.getProductId(),
                request.quantity(),
                product.getPrice(),
                product.getProductName()
        );
    }

    private void validateOrderableProduct(UUID storeId, Product product) {
        if (!product.getStoreId().equals(storeId)
                || product.isHidden()
                || product.isSoldOut()) {
            throw new ProductNotOrderableException();
        }
    }

    private String buildDeliveryAddressSnapshot(Address address) {
        StringBuilder snapshot = new StringBuilder();

        if (address.getZipCode() != null) {
            snapshot.append("[").append(address.getZipCode()).append("] ");
        }
        snapshot.append(address.getAddress());
        if (address.getDetail() != null) {
            snapshot.append(" ").append(address.getDetail());
        }

        // 주문 당시 배송지 문자열을 스냅샷으로 보관해 이후 주소 변경과 분리한다.
        return snapshot.toString();
    }

    private void validateMinOrderAmount(Order order, Store store) {
        if (order.getTotalPrice() < store.getMinOrderAmount()) {
            throw new MinOrderAmountNotMetException();
        }
    }

    private Order getOwnedOrder(Long userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        if (!order.getUserId().equals(userId)) {
            throw new OrderForbiddenException();
        }
        return order;
    }
}
