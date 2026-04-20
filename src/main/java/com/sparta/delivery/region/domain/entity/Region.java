package com.sparta.delivery.region.domain.entity;


import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "p_region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
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
}
