package com.sparta.delivery.order.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.order.application.OrderService;
import com.sparta.delivery.order.domain.entity.OrderStatus;
import com.sparta.delivery.order.presentation.dto.OrderCreateRequest;
import com.sparta.delivery.order.presentation.dto.OrderDetailResponse;
import com.sparta.delivery.order.presentation.dto.OrderListResponse;
import com.sparta.delivery.order.presentation.dto.OrderStatusUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "주문 API")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderDetailResponse response = orderService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "주문 목록 조회")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderListResponse>>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<OrderListResponse> response = orderService.getOrders(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                storeId,
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "주문 상세 조회")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        OrderDetailResponse response = orderService.getOrder(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "주문 취소")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        orderService.cancel(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "주문 상태 변경")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        OrderDetailResponse response = orderService.updateStatus(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                orderId,
                request.status()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "주문 삭제")
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        orderService.delete(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
