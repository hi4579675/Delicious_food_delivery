package com.sparta.delivery.payment.presentation.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.payment.application.service.PaymentService;
import com.sparta.delivery.payment.presentation.dto.PaymentCreateRequest;
import com.sparta.delivery.payment.presentation.dto.PaymentResponse;
import com.sparta.delivery.payment.presentation.dto.PaymentStatusUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Payment", description = "결제 API")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 생성")
    @PostMapping("/payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.create(principal.getId(), request)));
    }

    @Operation(summary = "결제 단건 조회")
    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByPaymentId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        UserRole actorRole = UserRole.valueOf(principal.getRole());
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getByPaymentId(principal.getId(), actorRole, paymentId)
        ));
    }

    @Operation(summary = "결제 전체 조회")
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction

    ) {
        int normalizedSize = (size == 10 || size == 30 || size == 50) ? size : 10;
        UserRole actorRole = UserRole.valueOf(principal.getRole());

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPayments(principal.getId(), actorRole, page, normalizedSize, sortBy, direction)
        ));
    }

    @Operation(summary = "결제 삭제")
    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        paymentService.delete(principal.getId(), UserRole.valueOf(principal.getRole()), paymentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "결제 상태 변경")
    @PutMapping("/payments/{paymentId}/status")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.updateStatus(principal.getId(), paymentId, request)
        ));
    }
}
