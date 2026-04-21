package com.sparta.delivery.product.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.product.domain.exception.*;
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
        validateDescriptionForCreate(description, descriptionSource);
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
            Integer price,
            Integer displayOrder
    ) {
        validateProductName(productName);
        validateDescriptionForUpdate(description);
        validatePrice(price);
        validateDisplayOrder(displayOrder);

        this.productName = productName;
        this.description = description;
        this.descriptionSource = (description == null) ? null : DescriptionSource.MANUAL;
        this.price = price;
        this.displayOrder = displayOrder;
    }

    public void changeHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public void changeSoldOut(boolean soldOut) {
        this.isSoldOut = soldOut;
    }

    // not null
    private static void validateStoreId(UUID storeId) {
        if (storeId == null) {
            throw new InvalidStoreIdException();
        }
    }

    // not null, non-blank, and max length 100
    private static void validateProductName(String productName) {
        if (productName == null || productName.isBlank() || productName.length() > 100) {
            throw new InvalidProductNameException();
        }

    }

    // relations between description and description source
    private static void validateDescriptionForCreate(String description, DescriptionSource descriptionSource) {
        if (description == null && descriptionSource != null) {
            throw new InvalidProductDescriptionException();
        }

        if (description != null && descriptionSource == null) {
            throw new InvalidProductDescriptionException();
        }
    }

    // nullable, but blank-only description is not allowed
    private static void validateDescriptionForUpdate(String description) {
        if (description != null && description.isBlank()) {
            throw new InvalidProductDescriptionException();
        }
    }

    // not null and greater than 0
    private static void validatePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new InvalidProductPriceException();
        }
    }

    // greater than or equal to 0
    private static void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new InvalidDisplayOrderException();
        }
    }
}
