package com.sparta.delivery.payment.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sparta.delivery.common.model.BaseEntity;

@Entity
@Table(name = "p_payment", uniqueConstraints = {@UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "pg_provider", length = 50)
    private String pgProvider;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Builder
    private Payment(UUID orderId, PaymentMethod paymentMethod, Integer totalPrice, LocalDateTime approvedAt, LocalDateTime failedAt, LocalDateTime cancelledAt, String failureReason, String pgProvider, String pgTransactionId) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = PaymentStatus.PENDING;
        this.totalPrice = totalPrice;
        this.approvedAt = approvedAt;
        this.failedAt = failedAt;
        this.cancelledAt = cancelledAt;
        this.failureReason = failureReason;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
    }

    public void approve(String pgProvider, String pgTransactionId) {
        if(this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException();
        }

        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
    }

    public void fail(String failureReason) {

        if(this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException();
        }

        this.paymentStatus = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = failureReason;
    }

    public void cancel() {

        if(this.paymentStatus != PaymentStatus.APPROVED) {
            throw new IllegalStateException();
        }

        this.cancelledAt = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

}
