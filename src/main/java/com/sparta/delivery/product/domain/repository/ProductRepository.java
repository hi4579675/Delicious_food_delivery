package com.sparta.delivery.product.domain.repository;

import com.sparta.delivery.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductId(UUID productId);

    /* 기본 목록 조회와 노출 순서(displayOrder) 기준 목록 조회를 분리한다.
    서비스/유스케이스에 따라 정렬 없는 원본 목록이 필요할 수 있고,
    사용자 노출용 목록은 displayOrder 기준 정렬이 필요하다. */

    List<Product> findAllByStoreId(UUID storeId);

    List<Product> findAllByStoreIdOrderByDisplayOrderAsc(UUID storeId);

    List<Product> findAllByStoreIdAndIsHiddenFalseOrderByDisplayOrderAsc(UUID storeId);

    boolean existsByStoreIdAndProductName(UUID storeId, String productName);
}
