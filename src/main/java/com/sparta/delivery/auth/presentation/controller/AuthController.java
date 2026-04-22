package com.sparta.delivery.auth.presentation;

import com.sparta.delivery.auth.application.AuthService;
import com.sparta.delivery.auth.presentation.dto.LoginResponse;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.presentation.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    
}
