package com.sparta.delivery.region.domain.entity;


import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.region.domain.exception.InvalidRegionActiveStatusException;
import com.sparta.delivery.region.domain.exception.InvalidRegionCodeException;
import com.sparta.delivery.region.domain.exception.InvalidRegionDepthException;
import com.sparta.delivery.region.domain.exception.InvalidRegionNameException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_region")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {
    private static final String REGION_CODE_PATTERN = "^\\d{10}$";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "region_id", updatable = false, nullable = false)
    private UUID regionId;

    @Column(name = "region_code", updatable = false, nullable = false)
    private String regionCode;

    @Column(name = "region_name", nullable = false, length = 100)
    private String regionName;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Region(String regionCode, String regionName, UUID parentId, Integer depth, Boolean isActive) {
        validateRegionCode(regionCode);
        validateRegionName(regionName);
        validateDepth(depth);
        validateIsActive(isActive);

        this.regionCode = regionCode;
        this.regionName = regionName;
        this.parentId = parentId;
        this.depth = depth;
        this.isActive = isActive;
    }

    public static Region create(String regionCode, String regionName, UUID parentId, Integer depth, Boolean isActive) {
        return new Region(regionCode, regionName, parentId, depth, isActive);
    }

    public void update(
            String regionName,
            UUID parentId,
            Integer depth,
            Boolean isActive
    ) {
        validateRegionName(regionName);
        validateDepth(depth);
        validateIsActive(isActive);

        this.regionName = regionName;
        this.parentId = parentId;
        this.depth = depth;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    private static void validateRegionCode(String regionCode) {
        if (regionCode == null || !regionCode.matches(REGION_CODE_PATTERN)) {
            throw new InvalidRegionCodeException();
        }
    }

    private static void validateRegionName(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            throw new InvalidRegionNameException();
        }
    }

    private static void validateDepth(Integer depth) {
        if (depth == null || depth < 1) {
            throw new InvalidRegionDepthException();
        }
    }

    private static void validateIsActive(Boolean isActive) {
        if (isActive == null) {
            throw new InvalidRegionActiveStatusException();
        }
    }
}
