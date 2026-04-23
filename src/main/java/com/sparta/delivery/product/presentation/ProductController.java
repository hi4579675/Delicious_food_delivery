package com.sparta.delivery.product.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.product.application.ProductService;
import com.sparta.delivery.product.presentation.dto.request.ProductCreateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductHiddenUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductSoldOutUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductUpdateRequest;
import com.sparta.delivery.product.presentation.dto.response.ProductResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Product", description = "상품 API")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 등록")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PostMapping("/stores/{storeId}/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID storeId,
            @RequestBody @Valid ProductCreateRequest request
            ) {
        ProductResponse response = productService.create(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                storeId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(summary = "상품 단건 조회")
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId
    ) {

        Long actorId = principal == null ? null : principal.getId();
        UserRole actorRole = principal == null ? null : UserRole.valueOf(principal.getRole());
        ProductResponse response = productService.getProduct(actorId, actorRole, productId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 목록 조회")
    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID storeId
    ) {
        Long actorId = principal == null ? null : principal.getId();
        UserRole actorRole = principal == null ? null : UserRole.valueOf(principal.getRole());

        List<ProductResponse> response = productService.getProducts(
                actorId,
                actorRole,
                storeId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "상품 일반 정보 수정")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @RequestBody @Valid ProductUpdateRequest request
            ) {
        ProductResponse response = productService.update(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                productId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 숨김 상태 변경")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/products/{productId}/hidden")
    public ResponseEntity<ApiResponse<ProductResponse>> changeHidden(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @RequestBody ProductHiddenUpdateRequest request
            ) {
        ProductResponse response = productService.changeHidden(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                productId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 품절 상태 변경")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/products/{productId}/sold-out")
    public ResponseEntity<ApiResponse<ProductResponse>> changeSoldOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @RequestBody ProductSoldOutUpdateRequest request
            ) {
        ProductResponse response = productService.changeSoldOut(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                productId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 삭제")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId
    ) {
        ProductResponse response = productService.delete(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                productId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
