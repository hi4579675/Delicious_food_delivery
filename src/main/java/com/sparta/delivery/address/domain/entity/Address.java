package com.sparta.delivery.address.domain.entity;

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

    @Builder
    private Address(
            Long userId,
            String alias,
            String address,
            String detail,
            String zipCode,
            boolean isDefault
    ) {
        this.userId = userId;
        this.alias = alias;
        this.address = address;
        this.detail = detail;
        this.zipCode = zipCode;
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
        this.alias = alias;
        this.address = address;
        this.detail = detail;
        this.zipCode = zipCode;
    }
}
