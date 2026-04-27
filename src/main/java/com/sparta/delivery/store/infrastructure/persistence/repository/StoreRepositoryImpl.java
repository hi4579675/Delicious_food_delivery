package com.sparta.delivery.store.infrastructure.persistence.repository;

import static com.querydsl.core.types.Order.ASC;
import static com.querydsl.core.types.Order.DESC;
import static com.sparta.delivery.store.domain.entity.QStore.store;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepositoryCustom;
import com.sparta.delivery.store.presentation.dto.StoreSearchCondition;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Store> searchStores(StoreSearchCondition condition, Pageable pageable) {
        StoreSearchCondition normalizedCondition = condition == null
                ? new StoreSearchCondition(null, null, null, null, null, null, null, null, null, null, null)
                : condition;

        BooleanBuilder builder = new BooleanBuilder();

        if (normalizedCondition.regionId() != null) {
            builder.and(store.regionId.eq(normalizedCondition.regionId()));
        }

        if (normalizedCondition.categoryId() != null) {
            builder.and(store.categoryId.eq(normalizedCondition.categoryId()));
        }

        if (normalizedCondition.userId() != null) {
            builder.and(store.userId.eq(normalizedCondition.userId()));
        }

        if (normalizedCondition.isOpen() != null) {
            builder.and(store.isOpen.eq(normalizedCondition.isOpen()));
        }

        if (normalizedCondition.keyword() != null && !normalizedCondition.keyword().isBlank()) {
            builder.and(store.storeName.containsIgnoreCase(normalizedCondition.keyword().trim()));
        }

        if (normalizedCondition.addressKeyword() != null && !normalizedCondition.addressKeyword().isBlank()) {
            builder.and(store.address.containsIgnoreCase(normalizedCondition.addressKeyword().trim()));
        }

        if (normalizedCondition.minRating() != null) {
            builder.and(store.avgRating.goe(normalizedCondition.minRating()));
        }

        if (normalizedCondition.minReviewCount() != null) {
            builder.and(store.reviewCount.goe(normalizedCondition.minReviewCount()));
        }

        if (normalizedCondition.maxMinOrderAmount() != null) {
            builder.and(store.minOrderAmount.loe(normalizedCondition.maxMinOrderAmount()));
        }

        if (normalizedCondition.createdAfter() != null) {
            builder.and(store.createdAt.goe(normalizedCondition.createdAfter()));
        }

        if (normalizedCondition.createdBefore() != null) {
            builder.and(store.createdAt.loe(normalizedCondition.createdBefore()));
        }

        List<Store> content = queryFactory
                .selectFrom(store)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        Long total = queryFactory
                .select(store.count())
                .from(store)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            com.querydsl.core.types.Order direction = order.isAscending() ? ASC : DESC;

            switch (order.getProperty()) {
                case "createdAt" -> orders.add(new OrderSpecifier<>(direction, store.createdAt));
                case "avgRating" -> orders.add(new OrderSpecifier<>(direction, store.avgRating));
                case "reviewCount" -> orders.add(new OrderSpecifier<>(direction, store.reviewCount));
                case "minOrderAmount" -> orders.add(new OrderSpecifier<>(direction, store.minOrderAmount));
                default -> {
                }
            }
        }

        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(DESC, store.createdAt));
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
