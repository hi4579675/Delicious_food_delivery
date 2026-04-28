package com.sparta.delivery.order.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.order.domain.exception.CancelTimeExceededException;
import com.sparta.delivery.order.domain.exception.InvalidCancelDeadlineException;
import com.sparta.delivery.order.domain.exception.InvalidDeliveryAddressSnapshotException;
import com.sparta.delivery.order.domain.exception.InvalidOrderException;
import com.sparta.delivery.order.domain.exception.InvalidOrderItemException;
import com.sparta.delivery.order.domain.exception.InvalidOrderStatusException;
import com.sparta.delivery.order.domain.exception.InvalidOrderTotalPriceException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_order")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    private static final long CANCEL_WINDOW_MINUTES = 5L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false, updatable = false)
    private UUID storeId;

    @Column(nullable = false, updatable = false)
    private UUID addressId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, updatable = false, length = 500)
    private String deliveryAddressSnapshot;

    private LocalDateTime rejectedAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime cancelDeadlineAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Order(
            UUID storeId,
            UUID addressId,
            Long userId,
            Integer totalPrice,
            OrderStatus status,
            String deliveryAddressSnapshot,
            LocalDateTime rejectedAt,
            LocalDateTime cancelDeadlineAt,
            LocalDateTime completedAt,
            LocalDateTime canceledAt
    ) {
        this.storeId = requireStoreId(storeId);
        this.addressId = requireAddressId(addressId);
        this.userId = requireUserId(userId);
        this.totalPrice = requireTotalPrice(totalPrice);
        this.status = requireStatus(status);
        this.deliveryAddressSnapshot = requireDeliveryAddressSnapshot(deliveryAddressSnapshot);
        this.cancelDeadlineAt = requireCancelDeadline(cancelDeadlineAt);
        this.rejectedAt = rejectedAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
    }

    public static Order create(
            UUID storeId,
            UUID addressId,
            Long userId,
            String deliveryAddressSnapshot,
            List<OrderItem> items
    ) {
        requireOrderItems(items);

        Order order = Order.builder()
                .storeId(storeId)
                .addressId(addressId)
                .userId(userId)
                .totalPrice(calculateTotalPrice(items))
                .status(OrderStatus.PENDING)
                .deliveryAddressSnapshot(deliveryAddressSnapshot)
                .cancelDeadlineAt(LocalDateTime.now().plusMinutes(CANCEL_WINDOW_MINUTES))
                .build();

        for (OrderItem item : items) {
            order.addOrderItem(item);
        }

        return order;
    }

    public void addOrderItem(OrderItem item) {
        requireOrderItem(item);
        this.items.add(item);
        item.setOrder(this);
        this.totalPrice = calculateTotalPrice(this.items);
    }

    public void accept() {
        requireCurrentStatus(OrderStatus.PENDING);
        this.status = OrderStatus.ACCEPTED;
    }

    public void startCooking() {
        requireCurrentStatus(OrderStatus.ACCEPTED);
        this.status = OrderStatus.COOKING;
    }

    public void startDelivery() {
        requireCurrentStatus(OrderStatus.COOKING);
        this.status = OrderStatus.DELIVERING;
    }

    public void markDelivered() {
        requireCurrentStatus(OrderStatus.DELIVERING);
        this.status = OrderStatus.DELIVERED;
    }

    public void complete() {
        requireCurrentStatus(OrderStatus.DELIVERED);
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void reject() {
        requireCurrentStatus(OrderStatus.PENDING);
        this.status = OrderStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (isCancelDeadlineExceeded()) {
            throw new CancelTimeExceededException();
        }
        if (status != OrderStatus.PENDING && status != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStatusException();
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public boolean isCancelDeadlineExceeded() {
        return LocalDateTime.now().isAfter(cancelDeadlineAt);
    }

    private static UUID requireStoreId(UUID storeId) {
        if (storeId == null) {
            throw new InvalidOrderException();
        }
        return storeId;
    }

    private static UUID requireAddressId(UUID addressId) {
        if (addressId == null) {
            throw new InvalidOrderException();
        }
        return addressId;
    }

    private static Long requireUserId(Long userId) {
        if (userId == null) {
            throw new InvalidOrderException();
        }
        return userId;
    }

    private static Integer requireTotalPrice(Integer totalPrice) {
        if (totalPrice == null || totalPrice <= 0) {
            throw new InvalidOrderTotalPriceException();
        }
        return totalPrice;
    }

    private static OrderStatus requireStatus(OrderStatus status) {
        if (status == null) {
            throw new InvalidOrderStatusException();
        }
        return status;
    }

    private static String requireDeliveryAddressSnapshot(String deliveryAddressSnapshot) {
        if (deliveryAddressSnapshot == null || deliveryAddressSnapshot.isBlank()) {
            throw new InvalidDeliveryAddressSnapshotException();
        }
        return deliveryAddressSnapshot;
    }

    private static LocalDateTime requireCancelDeadline(LocalDateTime cancelDeadlineAt) {
        if (cancelDeadlineAt == null) {
            throw new InvalidCancelDeadlineException();
        }
        return cancelDeadlineAt;
    }

    private void requireCurrentStatus(OrderStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new InvalidOrderStatusException();
        }
    }

    private static void requireOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderItemException();
        }
    }

    private static Integer calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getLineTotalPrice)
                .reduce(0, Integer::sum);
    }

    private void requireOrderItem(OrderItem item) {
        if (item == null || (item.getOrder() != null && item.getOrder() != this)) {
            throw new InvalidOrderItemException();
        }
    }
}
