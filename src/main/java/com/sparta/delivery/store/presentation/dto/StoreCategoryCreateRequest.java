package com.sparta.delivery.store.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreCategoryCreateRequest(

        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
        String categoryName,

        String description,

        @NotNull(message = "활성 상태는 필수입니다.")
        Boolean isActive
) {
}
