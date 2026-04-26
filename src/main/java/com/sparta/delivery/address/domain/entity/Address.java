package com.sparta.delivery.address.domain.entity;

import com.sparta.delivery.address.domain.exception.InvalidAddressException;
import com.sparta.delivery.address.domain.exception.InvalidUserIdException;
import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_address")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID addressId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(length = 50)
    private String alias;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 255)
    private String detail;

    @Column(length = 10)
    private String zipCode;

    @Column(nullable = false)
    private boolean isDefault;

    @Builder(access = AccessLevel.PRIVATE)
    private Address(
            Long userId,
            String alias,
            String address,
            String detail,
            String zipCode,
            boolean isDefault
    ) {
        this.userId = requireUserId(userId);
        this.alias = normalizeOptional(alias);
        this.address = requireAddress(address);
        this.detail = normalizeOptional(detail);
        this.zipCode = normalizeOptional(zipCode);
        this.isDefault = isDefault;
    }

    public static Address create(
            Long userId,
            String alias,
            String address,
            String detail,
            String zipCode,
            boolean isDefault
    ) {
        return Address.builder()
                .userId(userId)
                .alias(alias)
                .address(address)
                .detail(detail)
                .zipCode(zipCode)
                .isDefault(isDefault)
                .build();
    }

    public void update(String alias, String address, String detail, String zipCode) {
        this.alias = normalizeOptional(alias);
        this.address = requireAddress(address);
        this.detail = normalizeOptional(detail);
        this.zipCode = normalizeOptional(zipCode);
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    private static String requireAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAddressException();
        }
        return value.trim();
    }

    private static Long requireUserId(Long userId) {
        if (userId == null) {
            throw new InvalidUserIdException();
        }
        return userId;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
