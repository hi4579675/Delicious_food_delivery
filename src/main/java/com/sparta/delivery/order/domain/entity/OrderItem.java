package com.sparta.delivery.order.domain.entity;

import com.sparta.delivery.order.domain.exception.InvalidOrderItemException;
import com.sparta.delivery.order.domain.exception.InvalidOrderItemQuantityException;
import com.sparta.delivery.order.domain.exception.InvalidOrderItemUnitPriceException;
import com.sparta.delivery.order.domain.exception.InvalidProductNameSnapshotException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "p_order_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer lineTotalPrice;

    @Column(nullable = false, length = 255)
    private String productNameSnapshot;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @Builder
    private OrderItem(
            UUID productId,
            Integer quantity,
            Integer unitPrice,
            String productNameSnapshot
    ) {
        this.productId = requireProductId(productId);
        this.quantity = requireQuantity(quantity);
        this.unitPrice = requireUnitPrice(unitPrice);
        this.lineTotalPrice = this.quantity * this.unitPrice;
        this.productNameSnapshot = requireProductNameSnapshot(productNameSnapshot);
    }

    public static OrderItem create(
            UUID productId,
            Integer quantity,
            Integer unitPrice,
            String productNameSnapshot
    ) {
        return OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .productNameSnapshot(productNameSnapshot)
                .build();
    }

    void setOrder(Order order) {
        if (order == null || (this.order != null && this.order != order)) {
            throw new InvalidOrderItemException();
        }
        this.order = order;
    }

    private static UUID requireProductId(UUID productId) {
        if (productId == null) {
            throw new InvalidOrderItemException();
        }
        return productId;
    }

    private static Integer requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidOrderItemQuantityException();
        }
        return quantity;
    }

    private static Integer requireUnitPrice(Integer unitPrice) {
        if (unitPrice == null || unitPrice <= 0) {
            throw new InvalidOrderItemUnitPriceException();
        }
        return unitPrice;
    }

    private static String requireProductNameSnapshot(String productNameSnapshot) {
        if (productNameSnapshot == null || productNameSnapshot.isBlank()) {
            throw new InvalidProductNameSnapshotException();
        }
        return productNameSnapshot;
    }
}
