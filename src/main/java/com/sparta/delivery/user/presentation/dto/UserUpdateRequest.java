package com.sparta.delivery.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 4, max = 10, message = "이름은 4자 이상 10자 이하입니다.")
        @Pattern(regexp = "^[a-z0-9]+$", message = "이름은 알파벳 소문자와 숫자만 가능합니다.")
        String name,

        @Pattern(
                regexp = "^01\\d-?\\d{3,4}-?\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone,

        boolean isPublic,

        /** AI 설명 생성 기능 전역 활성화 여부 (OWNER 전용, CUSTOMER 는 무시됨) */
        boolean useAiDescription
) {
        public UserUpdateRequest {
                phone = (phone == null || phone.isBlank()) ? null : phone.strip();
        }
}