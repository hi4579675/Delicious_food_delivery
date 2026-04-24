package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, UUID> {

    boolean existsByCategoryName(String categoryName);

    boolean existsBySortOrder(Integer sortOrder);

    Optional<StoreCategory> findByCategoryId(UUID categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<StoreCategory> findAllByOrderBySortOrderDesc(Pageable pageable);

    List<StoreCategory> findAllByOrderBySortOrderAsc();

    List<StoreCategory> findAllByIsActiveTrueOrderBySortOrderAsc();
}
