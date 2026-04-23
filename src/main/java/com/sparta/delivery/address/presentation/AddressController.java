package com.sparta.delivery.address.presentation;

import com.sparta.delivery.address.application.AddressService;
import com.sparta.delivery.address.presentation.dto.AddressCreateRequest;
import com.sparta.delivery.address.presentation.dto.AddressResponse;
import com.sparta.delivery.address.presentation.dto.AddressUpdateRequest;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "배송지 API")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "배송지 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid AddressCreateRequest request
    ) {
        AddressResponse response = addressService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "내 배송지 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<AddressResponse> response = addressService.getAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송지 상세 조회")
    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        AddressResponse response = addressService.getAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송지 수정")
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @RequestBody @Valid AddressUpdateRequest request
    ) {
        AddressResponse response = addressService.update(principal.getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "기본 배송지 설정")
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        AddressResponse response = addressService.setDefaultAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "배송지 삭제")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        addressService.delete(principal.getId(), UserRole.valueOf(principal.getRole()), addressId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
