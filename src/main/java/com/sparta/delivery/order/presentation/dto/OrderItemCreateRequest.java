package com.sparta.delivery.order.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderItemCreateRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @NotNull(message = "주문 수량은 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
