package com.sparta.delivery.region.domain.repository;

import com.sparta.delivery.region.domain.entity.Region;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, UUID> {

    boolean existsByRegionCodeAndDeletedAtIsNull(String regionCode);

    Optional<Region> findByRegionIdAndDeletedAtIsNull(UUID regionId);

    Optional<Region> findByRegionCodeAndDeletedAtIsNull(String regionCode);

    List<Region> findByParentIdAndDeletedAtIsNull(UUID parentId);

    List<Region> findByParentIdIsNullAndDeletedAtIsNull();
}
