package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, UUID> {

    boolean existsByCategoryName(String categoryName);

    boolean existsBySortOrder(Integer sortOrder);

    @Query(
            value = "select exists (select 1 from p_store_category where category_name = :categoryName)",
            nativeQuery = true
    )
    boolean existsByCategoryNameIncludingDeleted(@Param("categoryName") String categoryName);

    @Query(
            value = "select exists (select 1 from p_store_category where sort_order = :sortOrder)",
            nativeQuery = true
    )
    boolean existsBySortOrderIncludingDeleted(@Param("sortOrder") Integer sortOrder);

    Optional<StoreCategory> findByCategoryId(UUID categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<StoreCategory> findAllByOrderBySortOrderDesc(Pageable pageable);

    List<StoreCategory> findAllByOrderBySortOrderAsc();

    List<StoreCategory> findAllByIsActiveTrueOrderBySortOrderAsc();
}
