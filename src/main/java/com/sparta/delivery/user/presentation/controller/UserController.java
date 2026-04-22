package com.sparta.delivery.user.presentation.controller;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.application.UserService;
import com.sparta.delivery.user.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signUp(@RequestBody @Valid SignupRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(userId));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(principal.getId())));
    }

    @Operation(summary = "내 정보 수정")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        userService.updateInfo(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "비밀번호 변경 — 변경 후 기존 JWT 전부 무효화")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "회원 탈퇴 — soft delete, 동일 이메일 재가입 허용")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.withdraw(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "사용자 역할 변경 (관리자)")
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestBody @Valid RoleChangeRequest request
    ) {
        userService.changeRole(userId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
