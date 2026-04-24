package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, UUID> {

    boolean existsByCategoryName(String categoryName);

    boolean existsBySortOrder(Integer sortOrder);

    Optional<StoreCategory> findByCategoryId(UUID categoryId);

    Optional<StoreCategory> findTopByOrderBySortOrderDesc();

    List<StoreCategory> findAllByOrderBySortOrderAsc();

    List<StoreCategory> findAllByIsActiveTrueOrderBySortOrderAsc();
}
