package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByStoreIdAndDeletedAtIsNull(UUID storeId);

    List<Store> findByUserIdAndDeletedAtIsNull(Long userId);

    List<Store> findByRegionIdAndDeletedAtIsNull(UUID regionId);

    List<Store> findByCategoryIdAndDeletedAtIsNull(UUID categoryId);
}
