package com.sparta.delivery.payment.application.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.sparta.delivery.order.domain.entity.Order;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.domain.repository.OrderRepository;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.payment.domain.entity.Payment;
import com.sparta.delivery.payment.domain.exception.DuplicatePaymentOrderException;
import com.sparta.delivery.payment.domain.exception.InvalidOrderIdException;
import com.sparta.delivery.payment.domain.exception.InvalidOrderStatusForPaymentException;
import com.sparta.delivery.payment.domain.exception.InvalidTotalPriceException;
import com.sparta.delivery.payment.domain.exception.InvalidPaymentStatusTransitionException;
import com.sparta.delivery.payment.domain.exception.PaymentForbiddenException;
import com.sparta.delivery.payment.domain.exception.PaymentNotFoundException;
import com.sparta.delivery.payment.domain.repository.PaymentRepository;
import com.sparta.delivery.payment.presentation.dto.PaymentCreateRequest;
import com.sparta.delivery.payment.presentation.dto.PaymentResponse;
import com.sparta.delivery.payment.presentation.dto.PaymentStatusUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final Set<OrderStatus> PAYMENT_ALLOWED_ORDER_STATUSES =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.ACCEPTED);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse create(Long actorId, PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(InvalidOrderIdException::new);
        if (!order.getUserId().equals(actorId)) {
            throw new PaymentForbiddenException();
        }
        requireOrderStatusAllowedForPayment(order.getStatus());

        if (!order.getTotalPrice().equals(request.totalPrice())) {
            throw new InvalidTotalPriceException();
        }

        if (paymentRepository.existsAnyByOrderIdIncludingDeleted(order.getOrderId())) {
            throw new DuplicatePaymentOrderException();
        }

        Payment payment = Payment.create(
                order.getOrderId(),
                request.paymentMethod(),
                request.totalPrice()
        );

        try {
            Payment saved = paymentRepository.save(payment);
            log.info("결제 생성 완료 - actorId={}, orderId={}, paymentId={}", actorId, order.getOrderId(), saved.getPaymentId());
            return PaymentResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePaymentOrderException();
        }
    }

    public PaymentResponse getByPaymentId(Long actorId, UserRole actorRole, UUID paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (actorRole == UserRole.CUSTOMER) { // UserRole이 CUSTOMER이고,
            Order order = orderRepository.findById(payment.getOrderId()) // 해당 payment에 포함된 orderId가 order 테이블에 존재하고,
                    .orElseThrow(PaymentNotFoundException::new);

            if (!order.getUserId().equals(actorId)) { // 접근한 CUSTOMER의 userId가 해당 order의 userId 정보와 일치해야함 -> 이 중 하나라도 아니라면 에러 처리
                throw new PaymentForbiddenException();
            }
        }

        return PaymentResponse.from(payment);
    }

    public PageResponse<PaymentResponse> getPayments(
            Long actorId,
            UserRole actorRole,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(parseDirection(direction), sortBy)
        );

        Page<Payment> result;

        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            result = paymentRepository.findAll(pageable);
        } else if (actorRole == UserRole.CUSTOMER) {
            List<UUID> orderIds = orderRepository.findAllByUserIdOrderByCreatedAtDesc(actorId).stream()
                    .map(Order::getOrderId)
                    .toList();

            if (orderIds.isEmpty()) {
                return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
            }

            result = paymentRepository.findAllByOrderIdIn(orderIds, pageable);
        } else {
            throw new PaymentForbiddenException();
        }

        return PageResponse.from(result.map(PaymentResponse::from));
    }

    @Transactional
    public PaymentResponse updateStatus(Long actorId, UUID paymentId, PaymentStatusUpdateRequest request) {
        Payment payment = getPaymentOrThrow(paymentId);

        switch (request.paymentStatus()) {
            case APPROVED -> {
                Order order = orderRepository.findById(payment.getOrderId())
                        .orElseThrow(PaymentNotFoundException::new);
                requireOrderStatusAllowedForPayment(order.getStatus());
                payment.approve(request.pgProvider(), request.pgTransactionId());
            }
            case FAILED -> payment.fail(request.failureReason());
            case CANCELLED -> payment.cancel();
            case PENDING -> throw new InvalidPaymentStatusTransitionException();
        }

        log.info("결제 상태 변경 완료 - actorId={}, paymentId={}, status={}", actorId, paymentId, payment.getPaymentStatus());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void delete(Long actorId, UserRole actorRole, UUID paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (actorRole != UserRole.MASTER) {
            throw new PaymentForbiddenException();
        }

        payment.softDelete(actorId);
        log.info("결제 삭제 완료(MASTER) - actorId={}, paymentId={}", actorId, paymentId);
    }

    private Payment getPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(PaymentNotFoundException::new);
    }

    private void requireOrderStatusAllowedForPayment(OrderStatus orderStatus) {
        if (!PAYMENT_ALLOWED_ORDER_STATUSES.contains(orderStatus)) {
            throw new InvalidOrderStatusForPaymentException();
        }
    }

    private Sort.Direction parseDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

}
