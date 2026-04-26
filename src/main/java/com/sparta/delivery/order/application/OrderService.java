package com.sparta.delivery.order.application;

import com.sparta.delivery.address.application.AddressService;
import com.sparta.delivery.address.domain.entity.Address;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderItem;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.domain.exception.InvalidOrderStatusException;
import com.sparta.delivery.order.domain.exception.MinOrderAmountNotMetException;
import com.sparta.delivery.order.domain.exception.OrderForbiddenException;
import com.sparta.delivery.order.domain.exception.OrderNotFoundException;
import com.sparta.delivery.order.domain.exception.ProductNotFoundException;
import com.sparta.delivery.order.domain.exception.ProductNotOrderableException;
import com.sparta.delivery.order.domain.exception.StoreNotFoundException;
import com.sparta.delivery.order.domain.exception.StoreNotOrderableException;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.order.presentation.dto.OrderCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderDetailResponse;
import com.sparta.delivery.order.presentation.dto.OrderItemCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderListResponse;
import com.sparta.delivery.product.domain.entity.Product;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import com.sparta.delivery.user.domain.entity.UserRole;
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

    public PageResponse<OrderListResponse> getOrders(
            Long actorId,
            UserRole actorRole,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        Page<OrderListResponse> page = findOrders(actorId, actorRole, storeId, status, pageable)
                .map(OrderListResponse::from);
        return PageResponse.from(page);
    }

    public OrderDetailResponse getOrder(Long actorId, UserRole actorRole, UUID orderId) {
        return OrderDetailResponse.from(getAccessibleOrder(actorId, actorRole, orderId));
    }

    @Transactional
    public void cancel(Long actorId, UserRole actorRole, UUID orderId) {
        Order order = getCancelableOrder(actorId, actorRole, orderId);
        order.cancel();
        log.info("주문 취소 완료 - actorId={}, orderId={}", actorId, orderId);
    }

    @Transactional
    public OrderDetailResponse updateStatus(
            Long actorId,
            UserRole actorRole,
            UUID orderId,
            OrderStatus targetStatus
    ) {
        Order order = getManageableOrder(actorId, actorRole, orderId);
        changeOrderStatus(order, targetStatus);
        log.info("주문 상태 변경 완료 - actorId={}, orderId={}, status={}",
                actorId, orderId, order.getStatus());
        return OrderDetailResponse.from(order);
    }

    @Transactional
    public void delete(Long actorId, UserRole actorRole, UUID orderId) {
        Order order = getDeletableOrder(actorRole, orderId);
        order.softDelete(actorId);
        log.info("주문 삭제 완료 - actorId={}, orderId={}", actorId, orderId);
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

    private Page<Order> findOrders(
            Long actorId,
            UserRole actorRole,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        Pageable normalizedPageable = normalizePageable(pageable);
        // 조회 가능 범위는 역할별로 다르므로 CUSTOMER / OWNER / ADMIN 계열로 분기한다.
        return switch (actorRole) {
            case CUSTOMER -> findCustomerOrders(actorId, storeId, status, normalizedPageable);
            case OWNER -> findOwnerOrders(actorId, storeId, status, normalizedPageable);
            case MANAGER, MASTER -> findAdminOrders(storeId, status, normalizedPageable);
        };
    }

    private Page<Order> findCustomerOrders(
            Long userId,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        if (storeId != null && status != null) {
            return orderRepository.findAllByUserIdAndStoreIdAndStatus(
                    userId,
                    storeId,
                    status,
                    pageable
            );
        }
        if (storeId != null) {
            return orderRepository.findAllByUserIdAndStoreId(userId, storeId, pageable);
        }
        if (status != null) {
            return orderRepository.findAllByUserIdAndStatus(userId, status, pageable);
        }
        return orderRepository.findAllByUserId(userId, pageable);
    }

    private Page<Order> findOwnerOrders(
            Long actorId,
            UUID storeId,
            OrderStatus status,
            Pageable pageable
    ) {
        if (storeId != null) {
            if (!isOwnedStore(actorId, storeId)) {
                throw new OrderForbiddenException();
            }
            return findAdminOrders(storeId, status, pageable);
        }

        List<UUID> ownedStoreIds = findOwnedStoreIds(actorId);

        if (ownedStoreIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (status != null) {
            return orderRepository.findAllByStoreIdInAndStatus(ownedStoreIds, status, pageable);
        }
        return orderRepository.findAllByStoreIdIn(ownedStoreIds, pageable);
    }

    private List<UUID> findOwnedStoreIds(Long actorId) {
        return storeRepository.findByUserIdAndDeletedAtIsNull(actorId).stream()
                .map(Store::getStoreId)
                .toList();
    }

    private Page<Order> findAdminOrders(UUID storeId, OrderStatus status, Pageable pageable) {
        if (storeId != null && status != null) {
            return orderRepository.findAllByStoreIdAndStatus(storeId, status, pageable);
        }
        if (storeId != null) {
            return orderRepository.findAllByStoreId(storeId, pageable);
        }
        if (status != null) {
            return orderRepository.findAllByStatus(status, pageable);
        }
        return orderRepository.findAll(pageable);
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

    private void changeOrderStatus(Order order, OrderStatus targetStatus) {
        switch (targetStatus) {
            case ACCEPTED -> order.accept();
            case COOKING -> order.startCooking();
            case DELIVERING -> order.startDelivery();
            case DELIVERED -> order.markDelivered();
            case COMPLETED -> order.complete();
            case REJECTED -> order.reject();
            default -> throw new InvalidOrderStatusException();
        }
    }

    private Order getAccessibleOrder(Long actorId, UserRole actorRole, UUID orderId) {
        return switch (actorRole) {
            case CUSTOMER -> getOwnedOrder(actorId, orderId);
            case OWNER -> getOwnerOrder(actorId, orderId);
            case MANAGER, MASTER -> getOrderOrThrow(orderId);
        };
    }

    private Order getOwnerOrder(Long actorId, UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        if (!isOwnedStore(actorId, order.getStoreId())) {
            throw new OrderForbiddenException();
        }
        return order;
    }

    private Order getCancelableOrder(Long actorId, UserRole actorRole, UUID orderId) {
        // 취소는 CUSTOMER 본인 주문과 MASTER 예외 권한만 허용한다.
        return switch (actorRole) {
            case CUSTOMER -> getOwnedOrder(actorId, orderId);
            case MASTER -> getOrderOrThrow(orderId);
            default -> throw new OrderForbiddenException();
        };
    }

    private Order getDeletableOrder(UserRole actorRole, UUID orderId) {
        if (actorRole != UserRole.MASTER) {
            throw new OrderForbiddenException();
        }
        return getOrderOrThrow(orderId);
    }

    private Order getManageableOrder(Long actorId, UserRole actorRole, UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        // 상태 변경은 OWNER(본인 가게) 또는 MANAGER / MASTER만 가능하다.
        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            return order;
        }

        if (actorRole == UserRole.OWNER && isOwnedStore(actorId, order.getStoreId())) {
            return order;
        }

        throw new OrderForbiddenException();
    }

    private Order getOwnedOrder(Long userId, UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new OrderForbiddenException();
        }
        return order;
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
    }

    private boolean isOwnedStore(Long actorId, UUID storeId) {
        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(StoreNotFoundException::new);
        return store.getUserId().equals(actorId);
    }
}
