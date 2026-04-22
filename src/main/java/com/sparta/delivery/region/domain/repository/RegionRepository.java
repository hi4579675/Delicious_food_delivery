package com.sparta.delivery.region.domain.repository;

import com.sparta.delivery.region.domain.entity.Region;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, UUID> {

    boolean existsByRegionCode(String regionCode);

    Optional<Region> findByRegionId(UUID regionId);

    Optional<Region> findByRegionCode(String regionCode);

    List<Region> findByParentId(UUID parentId);

    List<Region> findByParentIdIsNull();
}
