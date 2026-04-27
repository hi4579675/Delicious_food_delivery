package com.sparta.delivery.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderItem;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.payment.domain.entity.Payment;
import com.sparta.delivery.payment.domain.entity.PaymentStatus;
import com.sparta.delivery.payment.domain.entity.PaymentMethod;
import com.sparta.delivery.payment.domain.exception.DuplicatePaymentOrderException;
import com.sparta.delivery.payment.domain.exception.InvalidOrderIdException;
import com.sparta.delivery.payment.domain.exception.InvalidOrderStatusForPaymentException;
import com.sparta.delivery.payment.domain.exception.InvalidTotalPriceException;
import com.sparta.delivery.payment.domain.exception.PaymentForbiddenException;
import com.sparta.delivery.payment.domain.exception.PaymentNotFoundException;
import com.sparta.delivery.payment.domain.repository.PaymentRepository;
import com.sparta.delivery.payment.presentation.dto.PaymentCreateRequest;
import com.sparta.delivery.payment.presentation.dto.PaymentResponse;
import com.sparta.delivery.payment.presentation.dto.PaymentStatusUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("결제 생성")
    class Create {

        @Test
        @DisplayName("주문 소유자면 결제가 생성된다")
        void create_success() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, actorId, 10_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(paymentRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(false);

            UUID paymentId = UUID.randomUUID();
            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                ReflectionTestUtils.setField(p, "paymentId", paymentId);
                return p;
            });

            // when
            PaymentResponse response = paymentService.create(actorId, request);

            // then
            assertThat(response.paymentId()).isEqualTo(paymentId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(response.totalPrice()).isEqualTo(10_000);
            then(paymentRepository).should().save(any(Payment.class));
        }

        @Test
        @DisplayName("주문 상태가 결제 생성 허용 상태가 아니면 InvalidOrderStatusForPaymentException")
        void create_fail_whenOrderStatusNotAllowed() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, actorId, 10_000);
            order.accept();
            order.startCooking(); // COOKING
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(InvalidOrderStatusForPaymentException.class);

            then(paymentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("주문이 없으면 InvalidOrderIdException")
        void create_fail_whenOrderNotFound() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(InvalidOrderIdException.class);

            then(paymentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("주문 소유자가 아니면 PaymentForbiddenException")
        void create_fail_whenNotOwner() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, 2L, 10_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(PaymentForbiddenException.class);

            then(paymentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("요청 금액이 주문 총액과 다르면 InvalidTotalPriceException")
        void create_fail_whenTotalPriceMismatch() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, actorId, 9_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(InvalidTotalPriceException.class);

            then(paymentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 결제가 있으면 DuplicatePaymentOrderException")
        void create_fail_whenDuplicatePaymentExists() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, actorId, 10_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(paymentRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(DuplicatePaymentOrderException.class);

            then(paymentRepository).should().existsAnyByOrderIdIncludingDeleted(orderId);
            then(paymentRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("저장 시 무결성 예외가 나면 DuplicatePaymentOrderException")
        void create_fail_whenDataIntegrityViolation() {
            // given
            Long actorId = 1L;
            UUID orderId = UUID.randomUUID();
            PaymentCreateRequest request = new PaymentCreateRequest(orderId, PaymentMethod.CARD, 10_000);

            Order order = createOrder(orderId, actorId, 10_000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(paymentRepository.existsAnyByOrderIdIncludingDeleted(orderId)).willReturn(false);
            given(paymentRepository.save(any(Payment.class)))
                    .willThrow(new DataIntegrityViolationException("dup"));

            // when & then
            assertThatThrownBy(() -> paymentService.create(actorId, request))
                    .isInstanceOf(DuplicatePaymentOrderException.class);
        }
    }

    @Nested
    @DisplayName("결제 단건 조회")
    class GetByPaymentId {

        @Test
        @DisplayName("MANAGER는 결제를 단건 조회할 수 있다")
        void getByPaymentId_success_manager() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Payment payment = createPayment(paymentId, orderId, 10_000);

            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

            // when
            PaymentResponse response = paymentService.getByPaymentId(actorId, UserRole.MANAGER, paymentId);

            // then
            assertThat(response.paymentId()).isEqualTo(paymentId);
            assertThat(response.orderId()).isEqualTo(orderId);
            then(orderRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("결제가 없으면 PaymentNotFoundException")
        void getByPaymentId_fail_whenPaymentNotFound() {
            // given
            UUID paymentId = UUID.randomUUID();
            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getByPaymentId(1L, UserRole.MANAGER, paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("CUSTOMER는 본인 주문 결제만 조회할 수 있다")
        void getByPaymentId_fail_whenCustomerNotOwner() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Payment payment = createPayment(paymentId, orderId, 10_000);

            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(createOrder(orderId, 2L, 10_000)));

            // when & then
            assertThatThrownBy(() -> paymentService.getByPaymentId(actorId, UserRole.CUSTOMER, paymentId))
                    .isInstanceOf(PaymentForbiddenException.class);
        }

        @Test
        @DisplayName("CUSTOMER 조회 시 결제의 주문이 없으면 PaymentNotFoundException")
        void getByPaymentId_fail_whenCustomerOrderNotFound() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Payment payment = createPayment(paymentId, orderId, 10_000);

            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getByPaymentId(actorId, UserRole.CUSTOMER, paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("결제 목록 조회")
    class GetPayments {

        @Test
        @DisplayName("MANAGER는 전체 결제 목록을 조회할 수 있다")
        void getPayments_success_manager() {
            // given
            Payment p1 = createPayment(UUID.randomUUID(), UUID.randomUUID(), 10_000);
            Payment p2 = createPayment(UUID.randomUUID(), UUID.randomUUID(), 20_000);
            Page<Payment> page = new PageImpl<>(List.of(p1, p2));
            given(paymentRepository.findAll(any(Pageable.class))).willReturn(page);

            // when
            PageResponse<PaymentResponse> result = paymentService.getPayments(
                    1L,
                    UserRole.MANAGER,
                    0,
                    10,
                    "createdAt",
                    "desc"
            );

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(2);
            then(paymentRepository).should().findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("CUSTOMER가 주문이 없으면 빈 페이지를 반환한다")
        void getPayments_success_customer_noOrders() {
            // given
            Long actorId = 1L;
            given(orderRepository.findAllByUserIdOrderByCreatedAtDesc(actorId)).willReturn(List.of());

            // when
            PageResponse<PaymentResponse> result = paymentService.getPayments(
                    actorId,
                    UserRole.CUSTOMER,
                    0,
                    10,
                    "createdAt",
                    "desc"
            );

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.size()).isEqualTo(10);
            then(paymentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("CUSTOMER는 본인 주문들에 대한 결제 목록을 조회한다")
        void getPayments_success_customer_withOrders() {
            // given
            Long actorId = 1L;
            UUID orderId1 = UUID.randomUUID();
            UUID orderId2 = UUID.randomUUID();
            Order o1 = createOrder(orderId1, actorId, 10_000);
            Order o2 = createOrder(orderId2, actorId, 20_000);
            given(orderRepository.findAllByUserIdOrderByCreatedAtDesc(actorId))
                    .willReturn(List.of(o1, o2));

            Payment p1 = createPayment(UUID.randomUUID(), orderId1, 10_000);
            Payment p2 = createPayment(UUID.randomUUID(), orderId2, 20_000);
            given(paymentRepository.findAllByOrderIdIn(anyList(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(p1, p2)));

            // when
            PageResponse<PaymentResponse> result = paymentService.getPayments(
                    actorId,
                    UserRole.CUSTOMER,
                    0,
                    10,
                    "createdAt",
                    "desc"
            );

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            then(paymentRepository).should().findAllByOrderIdIn(eq(List.of(orderId1, orderId2)), any(Pageable.class));
        }

        @Test
        @DisplayName("허용되지 않는 역할이면 PaymentForbiddenException")
        void getPayments_fail_whenRoleNotAllowed() {
            assertThatThrownBy(() -> paymentService.getPayments(1L, UserRole.OWNER, 0, 10, "createdAt", "desc"))
                    .isInstanceOf(PaymentForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("결제 상태 변경")
    class UpdateStatus {

        @Test
        @DisplayName("주문 상태가 허용 상태면 결제를 승인할 수 있다")
        void updateStatus_success_approved() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            Payment payment = createPayment(paymentId, orderId, 10_000);
            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

            Order order = createOrder(orderId, actorId, 10_000); // PENDING
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(
                    PaymentStatus.APPROVED,
                    null,
                    "pg",
                    "tx"
            );

            // when
            PaymentResponse response = paymentService.updateStatus(actorId, paymentId, request);

            // then
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getApprovedAt()).isNotNull();
            then(orderRepository).should().findById(orderId);
        }

        @Test
        @DisplayName("주문 상태가 허용 상태가 아니면 결제 승인은 InvalidOrderStatusForPaymentException")
        void updateStatus_fail_approved_whenOrderStatusNotAllowed() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            Payment payment = createPayment(paymentId, orderId, 10_000);
            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

            Order order = createOrder(orderId, actorId, 10_000);
            order.accept();
            order.startCooking(); // COOKING
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(
                    PaymentStatus.APPROVED,
                    null,
                    "pg",
                    "tx"
            );

            // when & then
            assertThatThrownBy(() -> paymentService.updateStatus(actorId, paymentId, request))
                    .isInstanceOf(InvalidOrderStatusForPaymentException.class);

            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getApprovedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("결제 삭제")
    class Delete {

        @Test
        @DisplayName("MASTER는 결제를 삭제(soft delete)할 수 있다")
        void delete_success_master() {
            // given
            Long actorId = 99L;
            UUID paymentId = UUID.randomUUID();
            Payment payment = createPayment(paymentId, UUID.randomUUID(), 10_000);
            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

            // when
            paymentService.delete(actorId, UserRole.MASTER, paymentId);

            // then
            assertThat(payment.isDeleted()).isTrue();
            assertThat(payment.getDeletedBy()).isEqualTo(actorId);
            then(orderRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("MASTER가 아니면 PaymentForbiddenException")
        void delete_fail_whenNotMaster() {
            // given
            Long actorId = 1L;
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Payment payment = createPayment(paymentId, orderId, 10_000);

            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.delete(actorId, UserRole.CUSTOMER, paymentId))
                    .isInstanceOf(PaymentForbiddenException.class);

            // then
            assertThat(payment.isDeleted()).isFalse();
            then(orderRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제 대상 결제가 없으면 PaymentNotFoundException")
        void delete_fail_whenPaymentNotFound() {
            // given
            UUID paymentId = UUID.randomUUID();
            given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.delete(1L, UserRole.MASTER, paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    private static Order createOrder(UUID orderId, Long userId, Integer totalPrice) {
        OrderItem item = OrderItem.create(UUID.randomUUID(), 1, totalPrice, "product");
        Order order = Order.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                "snapshot",
                List.of(item)
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        return order;
    }

    private static Payment createPayment(UUID paymentId, UUID orderId, Integer totalPrice) {
        Payment payment = Payment.create(orderId, PaymentMethod.CARD, totalPrice);
        ReflectionTestUtils.setField(payment, "paymentId", paymentId);
        return payment;
    }
}
