package com.sparta.delivery.region.presentation.dto;

import com.sparta.delivery.region.domain.entity.Region;
import java.util.UUID;

public record RegionResponse(
        UUID regionId,
        String regionCode,
        String regionName,
        UUID parentId,
        Integer depth,
        Boolean isActive
) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getRegionId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getParentId(),
                region.getDepth(),
                region.getIsActive()
        );
    }
}
