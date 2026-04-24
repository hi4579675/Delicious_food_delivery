package com.sparta.delivery.store.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StoreUpdateRequest(

        @NotNull(message = "지역 ID는 필수입니다.")
        UUID regionId,

        @NotNull(message = "카테고리 ID는 필수입니다.")
        UUID categoryId,

        @NotBlank(message = "가게명은 필수입니다.")
        @Size(max = 100, message = "가게명은 100자 이하여야 합니다.")
        String storeName,

        String description,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String addressDetail,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phoneNumber,

        @NotNull(message = "최소 주문 금액은 필수입니다.")
        @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        Integer minOrderAmount,

        @NotNull(message = "영업 상태는 필수입니다.")
        Boolean isOpen,

        @NotNull(message = "활성 상태는 필수입니다.")
        Boolean isActive
) {
}
