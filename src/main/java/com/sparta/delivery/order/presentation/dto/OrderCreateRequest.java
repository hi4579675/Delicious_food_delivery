package com.sparta.delivery.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "가게 ID는 필수입니다.")
        UUID storeId,

        @NotNull(message = "배송지 ID는 필수입니다.")
        UUID addressId,

        @Valid
        @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다.")
        List<OrderItemCreateRequest> items
) {

    public OrderCreateRequest {
        items = (items == null) ? null : List.copyOf(items);
    }
}
