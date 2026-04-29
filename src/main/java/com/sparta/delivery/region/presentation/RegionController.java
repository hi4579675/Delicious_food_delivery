package com.sparta.delivery.region.presentation;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.region.application.RegionService;
import com.sparta.delivery.region.presentation.dto.RegionCreateRequest;
import com.sparta.delivery.region.presentation.dto.RegionResponse;
import com.sparta.delivery.region.presentation.dto.RegionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region", description = "지역 관리 API")
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "지역 생성")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<RegionResponse>> createRegion(
            @RequestBody @Valid RegionCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(regionService.createRegion(request)));
    }

    @Operation(summary = "지역 단건 조회")
    @GetMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionResponse>> getRegion(
            @PathVariable UUID regionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(regionService.getRegion(regionId)));
    }

    @Operation(summary = "지역 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RegionResponse>>> searchRegions(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(regionService.searchRegions(keyword, pageable)));
    }

    @Operation(summary = "비활성 지역 목록 조회")
    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<PageResponse<RegionResponse>>> getInactiveRegions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(regionService.getInactiveRegions(pageable)));
    }

    @Operation(summary = "최상위 지역 목록 조회")
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<RegionResponse>>> getRootRegions() {
        return ResponseEntity.ok(ApiResponse.success(regionService.getRootRegions()));
    }

    @Operation(summary = "하위 지역 목록 조회")
    @GetMapping("/{parentId}/children")
    public ResponseEntity<ApiResponse<List<RegionResponse>>> getChildRegions(
            @PathVariable UUID parentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(regionService.getChildRegions(parentId)));
    }

    @Operation(summary = "지역 수정")
    @PutMapping("/{regionId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<RegionResponse>> updateRegion(
            @PathVariable UUID regionId,
            @RequestBody @Valid RegionUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(regionService.updateRegion(regionId, request)));
    }

    @Operation(summary = "지역 삭제")
    @DeleteMapping("/{regionId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<Void>> deleteRegion(
            @PathVariable UUID regionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        regionService.deleteRegion(regionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
