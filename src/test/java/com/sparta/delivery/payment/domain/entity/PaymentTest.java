package com.sparta.delivery.payment.domain.entity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sparta.delivery.payment.domain.exception.InvalidOrderIdException;
import com.sparta.delivery.payment.domain.exception.InvalidPaymentMethodException;
import com.sparta.delivery.payment.domain.exception.InvalidPaymentStatusTransitionException;
import com.sparta.delivery.payment.domain.exception.InvalidTotalPriceException;

public class PaymentTest {

    @Test
    @DisplayName("create: 정상 생성 시 PENDING 상태로 생성된다.")
    void create_success() {
        Payment payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getTotalPrice()).isEqualTo(10000);
        assertThat(payment.getApprovedAt()).isNull();
        assertThat(payment.getFailedAt()).isNull();
        assertThat(payment.getCancelledAt()).isNull();
    }

    @Test
    void create_fail_whenOrderIdNull() {
        assertThatThrownBy(() -> Payment.create(null, PaymentMethod.CARD, 10000))
                .isInstanceOf(InvalidOrderIdException.class);
    }

    @Test
    void create_fail_whenPaymentMethodNull() {
        assertThatThrownBy(() -> Payment.create(UUID.randomUUID(), null, 10000))
                .isInstanceOf(InvalidPaymentMethodException.class);
    }

    @Test
    void crete_fail_whenTotalPriceInvalid() {
        assertThatThrownBy(() -> Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 0))
                .isInstanceOf(InvalidTotalPriceException.class);
    }

    @Test
    void approve_success_andNormalize() {
        Payment payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 10000);

        payment.approve(" toss ", "  ");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(payment.getPgProvider()).isEqualTo("toss");
        assertThat(payment.getPgTransactionId()).isNull();

    }

    @Test
    void fail_success_andNormalize() {
        Payment payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 1000);

        payment.fail("   ");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailedAt()).isNotNull();
        assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    void cancel_success_afterApprove() {
        Payment payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 1000);
        payment.approve("pg", "tx");

        payment.cancel();

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getCancelledAt()).isNotNull();
    }

    @Test
    void statusTransition_fail() {
        Payment payment = Payment.create(UUID.randomUUID(), PaymentMethod.CARD, 1000);
        payment.fail("reason");

        assertThatThrownBy(() -> payment.approve("pg", "tx"))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class);
    }

}
