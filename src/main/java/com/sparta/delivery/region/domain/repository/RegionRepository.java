package com.sparta.delivery.region.domain.repository;

import com.sparta.delivery.region.domain.entity.Region;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, UUID> {

    boolean existsByRegionCode(String regionCode);

    @Query(
            value = "select exists (select 1 from p_region where region_code = :regionCode)",
            nativeQuery = true
    )
    boolean existsByRegionCodeIncludingDeleted(@Param("regionCode") String regionCode);

    Optional<Region> findByRegionId(UUID regionId);

    Optional<Region> findByRegionCode(String regionCode);

    Page<Region> findByRegionNameContaining(String keyword, Pageable pageable);

    List<Region> findByParentId(UUID parentId);

    List<Region> findByParentIdIsNull();
}
