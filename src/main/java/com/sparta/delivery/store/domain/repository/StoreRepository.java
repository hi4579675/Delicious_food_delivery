package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID>, StoreRepositoryCustom {

    Optional<Store> findByStoreId(UUID storeId);

    List<Store> findByUserId(Long userId);

    List<Store> findByRegionId(UUID regionId);

    List<Store> findByCategoryId(UUID categoryId);

    List<Store> findByRegionIdAndCategoryId(UUID regionId, UUID categoryId);

    Page<Store> findAllByIsActiveFalse(Pageable pageable);
}
