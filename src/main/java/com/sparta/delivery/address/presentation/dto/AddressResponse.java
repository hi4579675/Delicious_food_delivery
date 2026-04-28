package com.sparta.delivery.address.presentation.dto;

import com.sparta.delivery.address.domain.entity.Address;
import java.time.LocalDateTime;
import java.util.UUID;

public record AddressResponse(
        UUID addressId,
        String alias,
        String address,
        String detail,
        String zipCode,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getAlias(),
                address.getAddress(),
                address.getDetail(),
                address.getZipCode(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
