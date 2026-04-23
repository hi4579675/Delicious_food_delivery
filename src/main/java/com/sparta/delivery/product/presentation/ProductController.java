package com.sparta.delivery.product.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.product.application.ProductService;
import com.sparta.delivery.product.presentation.dto.request.ProductCreateRequest;
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
}
