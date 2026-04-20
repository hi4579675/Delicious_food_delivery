package com.sparta.delivery.order.domain.entity;

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
            Order order,
            UUID productId,
            Integer quantity,
            Integer unitPrice,
            Integer lineTotalPrice,
            String productNameSnapshot
    ) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotalPrice = lineTotalPrice;
        this.productNameSnapshot = productNameSnapshot;
    }

    public static OrderItem create(
            Order order,
            UUID productId,
            Integer quantity,
            Integer unitPrice,
            Integer lineTotalPrice,
            String productNameSnapshot
    ) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineTotalPrice(lineTotalPrice)
                .productNameSnapshot(productNameSnapshot)
                .build();
    }

    void setOrder(Order order) {
        this.order = order;
    }
}
