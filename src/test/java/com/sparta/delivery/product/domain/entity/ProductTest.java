package com.sparta.delivery.product.domain.entity;

import com.sparta.delivery.product.domain.exception.InvalidDisplayOrderException;
import com.sparta.delivery.product.domain.exception.InvalidProductDescriptionException;
import com.sparta.delivery.product.domain.exception.InvalidProductNameException;
import com.sparta.delivery.product.domain.exception.InvalidProductPriceException;
import com.sparta.delivery.product.domain.exception.ProductErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("create should create product successfully with default states")
    void create_shouldCreateProductSuccessfully_withDefaultStates() {
        // given
        UUID storeId = UUID.randomUUID();
        String productName = "Americano";
        String description = "극강의 산미, 에티오피아 싱글 아메리카노";
        DescriptionSource descriptionSource = DescriptionSource.AI_GENERATED;
        Integer price = 4500;
        Integer displayOrder = 1;

        // when
        Product product = Product.create(
                storeId,
                productName,
                description,
                descriptionSource,
                price,
                displayOrder
        );

        // then
        assertThat(product.getStoreId()).isEqualTo(storeId);
        assertThat(product.getProductName()).isEqualTo(productName);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getDescriptionSource()).isEqualTo(descriptionSource);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getDisplayOrder()).isEqualTo(displayOrder);
        assertThat(product.isSoldOut()).isFalse();
        assertThat(product.isHidden()).isFalse();
    }

    @Test
    @DisplayName("create should allow null description and null descriptionSource")
    void create_shouldAllowNullDescriptionAndNullDescriptionSource() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Product product = Product.create(
                storeId,
                "Americano",
                null,
                null,
                4500,
                1
        );

        // then
        assertThat(product.getDescription()).isNull();
        assertThat(product.getDescriptionSource()).isNull();
    }

    @Test
    @DisplayName("create should throw when productName length exceeds 100")
    void create_shouldThrow_whenProductNameLengthExceeds100() {
        // given
        UUID storeId = UUID.randomUUID();
        String longName = "a".repeat(101);

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                longName,
                null,
                null,
                4500,
                1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProductNameException.class);
        InvalidProductNameException exception = (InvalidProductNameException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME.getCode());
    }

    @Test
    @DisplayName("create should throw when description is null and descriptionSource exists")
    void create_shouldThrow_whenDescriptionIsNullAndDescriptionSourceExists() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                "Americano",
                null,
                DescriptionSource.MANUAL,
                4500,
                1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProductDescriptionException.class);
        InvalidProductDescriptionException exception = (InvalidProductDescriptionException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_PRODUCT_DESCRIPTION.getCode());
    }

    @Test
    @DisplayName("create should throw when description exists and descriptionSource is null")
    void create_shouldThrow_whenDescriptionExistsAndDescriptionSourceIsNull() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                "Americano",
                "깔끔한 아메리카노",
                null,
                4500,
                1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProductDescriptionException.class);
        InvalidProductDescriptionException exception = (InvalidProductDescriptionException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_PRODUCT_DESCRIPTION.getCode());
    }

    @Test
    @DisplayName("create should allow description and descriptionSource together")
    void create_shouldAllowDescriptionAndDescriptionSourceTogether() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Product product = Product.create(
                storeId,
                "Americano",
                "깔끔한 아메리카노",
                DescriptionSource.AI_GENERATED,
                4500,
                1
        );

        // then
        assertThat(product.getDescription()).isEqualTo("깔끔한 아메리카노");
        assertThat(product.getDescriptionSource()).isEqualTo(DescriptionSource.AI_GENERATED);
    }

    @Test
    @DisplayName("create should throw when price is zero")
    void create_shouldThrow_whenPriceIsZero() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                "Americano",
                null,
                null,
                0,
                1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProductPriceException.class);
        InvalidProductPriceException exception = (InvalidProductPriceException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_PRODUCT_PRICE.getCode());
    }

    @Test
    @DisplayName("create should throw when price is negative")
    void create_shouldThrow_whenPriceIsNegative() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                "Americano",
                null,
                null,
                -100,
                1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidProductPriceException.class);
        InvalidProductPriceException exception = (InvalidProductPriceException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_PRODUCT_PRICE.getCode());
    }

    @Test
    @DisplayName("create should throw when displayOrder is negative")
    void create_shouldThrow_whenDisplayOrderIsNegative() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Throwable thrown = catchThrowable(() -> Product.create(
                storeId,
                "Americano",
                null,
                null,
                4500,
                -1
        ));

        // then
        assertThat(thrown).isInstanceOf(InvalidDisplayOrderException.class);
        InvalidDisplayOrderException exception = (InvalidDisplayOrderException) thrown;
        assertThat(exception.getCode()).isEqualTo(ProductErrorCode.INVALID_DISPLAY_ORDER.getCode());
    }

    @Test
    @DisplayName("create should allow null displayOrder")
    void create_shouldAllowNullDisplayOrder() {
        // given
        UUID storeId = UUID.randomUUID();

        // when
        Product product = Product.create(
                storeId,
                "Americano",
                null,
                null,
                4500,
                null
        );

        // then
        assertThat(product.getDisplayOrder()).isNull();
    }

    @Test
    @DisplayName("updateInfo should update fields and set descriptionSource to manual when description exists")
    void updateInfo_shouldUpdateFieldsAndSetDescriptionSourceToManual_whenDescriptionExists() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                "AI description",
                DescriptionSource.AI_GENERATED,
                4500,
                1
        );

        // when
        product.updateInfo(
                "Cafe Latte",
                "수기 설명",
                5500,
                2
        );

        // then
        assertThat(product.getProductName()).isEqualTo("Cafe Latte");
        assertThat(product.getDescription()).isEqualTo("수기 설명");
        assertThat(product.getDescriptionSource()).isEqualTo(DescriptionSource.MANUAL);
        assertThat(product.getPrice()).isEqualTo(5500);
        assertThat(product.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateInfo should set descriptionSource to null when description is null")
    void updateInfo_shouldSetDescriptionSourceToNull_whenDescriptionIsNull() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                "AI description",
                DescriptionSource.AI_GENERATED,
                4500,
                1
        );

        // when
        product.updateInfo(
                "Cafe Latte",
                null,
                5500,
                2
        );

        // then
        assertThat(product.getDescription()).isNull();
        assertThat(product.getDescriptionSource()).isNull();
    }

    @Test
    @DisplayName("changeHidden should change hidden state")
    void changeHidden_shouldChangeHiddenState() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                null,
                null,
                4500,
                1
        );

        // when
        product.changeHidden(true);

        // then
        assertThat(product.isHidden()).isTrue();

        // when
        product.changeHidden(false);

        // then
        assertThat(product.isHidden()).isFalse();
    }

    @Test
    @DisplayName("changeSoldOut should change soldOut state")
    void changeSoldOut_shouldChangeSoldOutState() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                null,
                null,
                4500,
                1
        );

        // when
        product.changeSoldOut(true);

        // then
        assertThat(product.isSoldOut()).isTrue();

        // when
        product.changeSoldOut(false);

        // then
        assertThat(product.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("softDelete should mark product as deleted")
    void softDelete_shouldMarkProductAsDeleted() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                null,
                null,
                4500,
                1
        );
        Long deletedBy = 1L;
        LocalDateTime before = LocalDateTime.now();

        // when
        product.softDelete(deletedBy);

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getDeletedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("softDelete should keep first deleted state when called twice")
    void softDelete_shouldKeepFirstDeletedState_whenCalledTwice() {
        // given
        Product product = Product.create(
                UUID.randomUUID(),
                "Americano",
                null,
                null,
                4500,
                1
        );

        // when
        product.softDelete(1L);
        LocalDateTime firstDeletedAt = product.getDeletedAt();
        Long firstDeletedBy = product.getDeletedBy();

        product.softDelete(2L);

        // then
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(product.getDeletedBy()).isEqualTo(firstDeletedBy);
    }
}