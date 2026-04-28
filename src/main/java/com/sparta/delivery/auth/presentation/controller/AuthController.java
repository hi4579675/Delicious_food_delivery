package com.sparta.delivery.auth.presentation.controller;

import com.sparta.delivery.auth.application.AuthService;
import com.sparta.delivery.auth.presentation.dto.LoginResponse;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.presentation.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sparta.delivery.common.config.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 인증 API.
 *
 * /api/v1/auth/** 는 SecurityConfig 에서 permitAll 설정.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 - accessToken 발급")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "로그아웃 — 해당 유저의 모든 기존 JWT 무효화 ")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
