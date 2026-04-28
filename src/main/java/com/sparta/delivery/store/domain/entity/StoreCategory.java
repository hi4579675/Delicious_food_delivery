package com.sparta.delivery.store.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.store.domain.exception.InvalidCategoryActiveStatusException;
import com.sparta.delivery.store.domain.exception.InvalidCategoryNameException;
import com.sparta.delivery.store.domain.exception.InvalidCategorySortOrderException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_store_category")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", updatable = false, nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    private String categoryName;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", unique = true)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder(access = AccessLevel.PRIVATE)
    private StoreCategory(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        String normalizedCategoryName = normalize(categoryName);
        String normalizedDescription = normalize(description);

        validateCategoryName(normalizedCategoryName);
        validateSortOrder(sortOrder);
        validateIsActive(isActive);

        this.categoryName = normalizedCategoryName;
        this.description = normalizedDescription;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    /** 가게 카테고리를 생성한다. */
    public static StoreCategory create(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        return StoreCategory.builder()
                .categoryName(categoryName)
                .description(description)
                .sortOrder(sortOrder)
                .isActive(isActive)
                .build();
    }

    /** 가게 카테고리 정보를 수정한다. */
    public void update(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        String normalizedCategoryName = normalize(categoryName);
        String normalizedDescription = normalize(description);

        validateCategoryName(normalizedCategoryName);
        validateSortOrder(sortOrder);
        validateIsActive(isActive);

        this.categoryName = normalizedCategoryName;
        this.description = normalizedDescription;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    /** 가게 카테고리를 활성 상태로 변경한다. */
    public void activate() {
        this.isActive = true;
    }

    /** 가게 카테고리를 비활성 상태로 변경한다. */
    public void deactivate() {
        this.isActive = false;
    }

    private static void validateCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new InvalidCategoryNameException();
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 0) {
            throw new InvalidCategorySortOrderException();
        }
    }

    private static void validateIsActive(Boolean isActive) {
        if (isActive == null) {
            throw new InvalidCategoryActiveStatusException();
        }
    }
}
