package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, UUID> {

    boolean existsByCategoryNameAndDeletedAtIsNull(String categoryName);

    Optional<StoreCategory> findByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    List<StoreCategory> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    List<StoreCategory> findAllByIsActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();
}
