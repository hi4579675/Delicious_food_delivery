package com.sparta.delivery.store.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "p_store_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreCategory extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "category_id", updatable = false, nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private StoreCategory(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        this.categoryName = categoryName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public static StoreCategory create(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        return new StoreCategory(categoryName, description, sortOrder, isActive);
    }

    public void update(
            String categoryName,
            String description,
            Integer sortOrder,
            Boolean isActive
    ) {
        this.categoryName = categoryName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
