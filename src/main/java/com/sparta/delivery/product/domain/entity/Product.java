package com.sparta.delivery.product.domain.entity;

import com.sparta.delivery.common.model.BaseEntity;
import com.sparta.delivery.product.domain.exception.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "p_product",
        indexes = {
                @Index(name = "idx_product_store_id", columnList = "store_id"),
                @Index(name = "idx_product_store_id_name", columnList = "store_id, product_name")
        }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Builder(access = AccessLevel.PRIVATE)
    private Product(
            UUID storeId,
            String productName,
            String description,
            DescriptionSource descriptionSource,
            Integer price,
            Integer displayOrder
    ) {
        this.storeId = storeId;
        this.productName = productName;
        this.description = description;
        this.descriptionSource = descriptionSource;
        this.price = price;
        this.isSoldOut = false;
        this.isHidden = false;
        this.displayOrder = displayOrder;
    }

    public static Product create(
            UUID storeId,
            String productName,
            String description,
            DescriptionSource descriptionSource,
            Integer price,
            Integer displayOrder
    ) {
        validateProductName(productName);
        validateDescriptionForCreate(description, descriptionSource);
        validatePrice(price);
        validateDisplayOrder(displayOrder);

        return Product.builder()
                .storeId(storeId)
                .productName(productName)
                .description(description)
                .descriptionSource(descriptionSource)
                .price(price)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateInfo(
            String productName,
            String description,
            Integer price,
            Integer displayOrder
    ) {
        validateProductName(productName);
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

    // max length 100
    private static void validateProductName(String productName) {
        if (productName.length() > 100) {
            throw new InvalidProductNameException();
        }
    }

    // description and descriptionSource must be both null or both non-null
    private static void validateDescriptionForCreate(String description, DescriptionSource descriptionSource) {
        if (description == null && descriptionSource != null) {
            throw new InvalidProductDescriptionException();
        }

        if (description != null && descriptionSource == null) {
            throw new InvalidProductDescriptionException();
        }
    }

    // greater than 0
    private static void validatePrice(Integer price) {
        if (price <= 0) {
            throw new InvalidProductPriceException();
        }
    }

    // nullable, but if present must be >= 0
    private static void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new InvalidDisplayOrderException();
        }
    }
}
