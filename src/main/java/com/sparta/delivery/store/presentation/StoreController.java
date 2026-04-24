package com.sparta.delivery.store.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.store.application.StoreService;
import com.sparta.delivery.store.presentation.dto.StoreCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreResponse;
import com.sparta.delivery.store.presentation.dto.StoreUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
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
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStores(
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(storeService.getStores(regionId, categoryId, userId)));
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
