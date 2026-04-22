package com.sparta.delivery.auth.presentation.dto;

/**
 * 로그인 응답 DTO.
 *
 * accessToken 만 반환 (refresh 토큰 현재 미사용)
 */
public record LoginResponse(
        String accessToken,
        Long userId,
        String role,
        long expiresIn
) {}