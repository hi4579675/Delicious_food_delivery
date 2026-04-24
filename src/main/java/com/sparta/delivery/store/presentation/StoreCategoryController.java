package com.sparta.delivery.store.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.store.application.StoreCategoryService;
import com.sparta.delivery.store.presentation.dto.StoreCategoryCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreCategoryResponse;
import com.sparta.delivery.store.presentation.dto.StoreCategoryUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "StoreCategory", description = "가게 카테고리 관리 API")
@RestController
@RequestMapping("/api/v1/store-categories")
@RequiredArgsConstructor
public class StoreCategoryController {

    private final StoreCategoryService storeCategoryService;

    @Operation(summary = "가게 카테고리 생성")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<StoreCategoryResponse>> createCategory(
            @RequestBody @Valid StoreCategoryCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(storeCategoryService.createCategory(request)));
    }

    @Operation(summary = "가게 카테고리 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(storeCategoryService.getCategories()));
    }

    @Operation(summary = "활성 가게 카테고리 목록 조회")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<StoreCategoryResponse>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success(storeCategoryService.getActiveCategories()));
    }

    @Operation(summary = "가게 카테고리 단건 조회")
    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<StoreCategoryResponse>> getCategory(
            @PathVariable UUID categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(storeCategoryService.getCategory(categoryId)));
    }

    @Operation(summary = "가게 카테고리 수정")
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<StoreCategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @RequestBody @Valid StoreCategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                storeCategoryService.updateCategory(categoryId, request)
        ));
    }

    @Operation(summary = "가게 카테고리 삭제")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        storeCategoryService.deleteCategory(categoryId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
