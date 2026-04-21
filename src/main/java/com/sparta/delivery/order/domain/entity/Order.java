package com.sparta.delivery.order.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
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
import java.util.ArrayList;
import java.time.LocalDateTime;
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
    private LocalDateTime cancelDeadlineAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    @Builder
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
        this.storeId = storeId;
        this.addressId = addressId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.deliveryAddressSnapshot = deliveryAddressSnapshot;
        this.rejectedAt = rejectedAt;
        this.cancelDeadlineAt = cancelDeadlineAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
    }

    public static Order create(
            UUID storeId,
            UUID addressId,
            Long userId,
            Integer totalPrice,
            String deliveryAddressSnapshot,
            LocalDateTime cancelDeadlineAt
    ) {
        return Order.builder()
                .storeId(storeId)
                .addressId(addressId)
                .userId(userId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .deliveryAddressSnapshot(deliveryAddressSnapshot)
                .cancelDeadlineAt(cancelDeadlineAt)
                .build();
    }

    public void addOrderItem(OrderItem item) {
        this.items.add(item);
        item.setOrder(this);
    }
}
