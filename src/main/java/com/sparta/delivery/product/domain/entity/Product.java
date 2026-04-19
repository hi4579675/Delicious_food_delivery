package com.sparta.delivery.product.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "description")
    private String description;

    @Column(name = "description_source")
    @Enumerated(EnumType.STRING)
    private DescriptionSource descriptionSource;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_sold_out", nullable = false)
    private boolean isSoldOut;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "display_order")
    private Integer displayOrder;

    public static Product create(
            UUID productId,
            UUID storeId,
            String productName,
            String description,
            DescriptionSource descriptionSource,
            Integer price,
            Integer displayOrder
    ) {
        validateStoreId(storeId);
        validateProductName(productName);
        validateDescription(description, descriptionSource);
        validatePrice(price);
        validateDisplayOrder(displayOrder);

        Product product = new Product();
        product.productId = productId;
        product.storeId = storeId;
        product.productName = productName;
        product.description = description;
        product.descriptionSource = descriptionSource;
        product.price = price;
        product.isSoldOut = false;
        product.isHidden = false;
        product.displayOrder = displayOrder;

        return product;
    }

    public void updateInfo(
            String productName,
            String description,
            DescriptionSource descriptionSource,
            Integer price,
            Integer displayOrder
    ) {
        validateProductName(productName);
        validateDescription(description, descriptionSource);
        validatePrice(price);
        validateDisplayOrder(displayOrder);

        this.productName = productName;
        this.description = description;
        this.descriptionSource = descriptionSource;
        this.price = price;
        this.displayOrder = displayOrder;
    }

    public void changeHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public void changeSoldOut(boolean soldOut) {
        this.isSoldOut = soldOut;
    }

    /*
    TODO: 모든 IllegalArgumentException은 상품 도메인 Exception으로 대체할 예정입니다.
     */

    // not null
    private static void validateStoreId(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("StoreId must not be null");
        }
    }

    // not null, non-blank and no whitespaces-only
    private static void validateProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("ProductName must not be blank");
        }

    }

    // relations between description and description source
    private static void validateDescription(String description, DescriptionSource descriptionSource) {
        if (description == null && descriptionSource != null) {
            throw new IllegalArgumentException("Description and DescriptionSource must match");
        }

        if (description != null && descriptionSource == null) {
            throw new IllegalArgumentException("Description and DescriptionSource must match");
        }
    }


    // not null and greater than 0
    private static void validatePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
    }

    // greater than or equal to 0
    private static void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new IllegalArgumentException("DisplayOrder must be greater than or equal to 0");
        }
    }
}
