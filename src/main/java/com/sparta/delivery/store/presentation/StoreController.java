package com.sparta.delivery.store.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.store.application.StoreService;
import com.sparta.delivery.store.presentation.dto.StoreCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreResponse;
import com.sparta.delivery.store.presentation.dto.StoreSearchCondition;
import com.sparta.delivery.store.presentation.dto.StoreUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Store", description = "가게 관리 API")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "가게 생성")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid StoreCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(storeService.createStore(principal.getId(), request)));
    }

    @Operation(summary = "가게 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StoreResponse>>> getStores(
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isOpen,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String addressKeyword,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minReviewCount,
            @RequestParam(required = false) Integer maxMinOrderAmount,
            @RequestParam(required = false) LocalDateTime createdAfter,
            @RequestParam(required = false) LocalDateTime createdBefore,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        StoreSearchCondition condition = new StoreSearchCondition(
                regionId,
                categoryId,
                userId,
                isOpen,
                keyword,
                addressKeyword,
                minRating,
                minReviewCount,
                maxMinOrderAmount,
                createdAfter,
                createdBefore
        );

        return ResponseEntity.ok(ApiResponse.success(storeService.searchStores(condition, pageable)));
    }

    @Operation(summary = "가게 단건 조회")
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(
            @PathVariable UUID storeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(storeService.getStore(storeId)));
    }

    @Operation(summary = "가게 수정")
    @PutMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid StoreUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                storeService.updateStore(
                        storeId,
                        principal.getId(),
                        UserRole.valueOf(principal.getRole()),
                        request
                )
        ));
    }

    @Operation(summary = "가게 삭제")
    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<Void>> deleteStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        storeService.deleteStore(
                storeId,
                principal.getId(),
                UserRole.valueOf(principal.getRole())
        );
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
