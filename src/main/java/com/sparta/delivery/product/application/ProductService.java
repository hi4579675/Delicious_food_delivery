package com.sparta.delivery.product.application;

import com.sparta.delivery.ai.application.AiDescriptionService;
import com.sparta.delivery.product.domain.entity.DescriptionSource;
import com.sparta.delivery.product.domain.entity.Product;
import com.sparta.delivery.product.domain.exception.DuplicateProductNameException;
import com.sparta.delivery.product.domain.exception.ProductForbiddenException;
import com.sparta.delivery.product.domain.exception.ProductNotFoundException;
import com.sparta.delivery.product.domain.repository.ProductRepository;
import com.sparta.delivery.product.presentation.dto.request.ProductCreateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductHiddenUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductSoldOutUpdateRequest;
import com.sparta.delivery.product.presentation.dto.request.ProductUpdateRequest;
import com.sparta.delivery.product.presentation.dto.response.ProductResponse;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import com.sparta.delivery.user.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final AiDescriptionService aiDescriptionService;

    @Transactional
    public ProductResponse create(Long actorId, UserRole actorRole, UUID storeId, ProductCreateRequest request) {
        Store store = getStoreOrThrow(storeId);
        validateProductWritePermission(actorId, actorRole, store);
        validateDuplicateProductName(storeId, request.productName());

        String description = resolveDescription(actorId, request);
        DescriptionSource descriptionSource = resolveDescriptionSource(request, description);

        Product product = createProduct(
                storeId,
                request,
                description,
                descriptionSource
        );

        Product savedProduct = productRepository.save(product);

        log.info("상품 등록 완료 - actorID={}, storeId={}, productId={}", actorId, storeId, savedProduct.getProductId());
        return ProductResponse.from(savedProduct);
    }

    public ProductResponse getProduct(Long actorId,  UserRole actorRole, UUID productId) {
        Product product = getProductOrThrow(productId);
        // hidden 상품이 아니라면 권한 관계없이 조회 가능
        if (!product.isHidden())
            {
                return ProductResponse.from(product);
            }

        // hidden 이라면 권한 별 분기 처리
        Store store = getStoreOrThrow(product.getStoreId());

        boolean canViewHidden =
                actorRole == UserRole.MANAGER
                || actorRole == UserRole.MASTER
                || (actorRole == UserRole.OWNER && store.getUserId().equals(actorId));

        if (!canViewHidden) {
            throw new ProductNotFoundException();
        }

        return ProductResponse.from(product);
    }

    public Page<ProductResponse> getProducts(
            Long actorId,
            UserRole actorRole,
            UUID storeId,
            int page,
            int size,
            String sort,
            String keyword
    ) {
        Store store = getStoreOrThrow(storeId);

        boolean canViewHidden =
                actorRole == UserRole.MANAGER
                || actorRole == UserRole.MASTER
                || (actorRole == UserRole.OWNER && store.getUserId().equals(actorId));

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                normalizeSort(sort)
        );

        String normalizedKeyword = normalizeKeyword(keyword);

        Page<Product> products =  getProductPage(storeId, normalizedKeyword, canViewHidden, pageable);

        return products.map(ProductResponse::from);
    }


    @Transactional
    public ProductResponse update(Long actorId, UserRole actorRole, UUID productId, ProductUpdateRequest request) {
        /**
         * 상품 일반 정보 수정. AI 자동 생성은 추후에도 반영하지 않음. AI설명 재생성 API를 따로 만들 예정.
         */
        Product product = getProductOrThrow(productId);
        Store store = getStoreOrThrow(product.getStoreId());
        validateProductWritePermission(actorId, actorRole, store);
        validateDuplicateProductNameOnUpdate(product, request.productName());
        product.updateInfo(
                request.productName(),
                request.description(),
                request.price(),
                request.displayOrder()
        );

        log.info("상품 수정 완료 - actorID={}, storeId={}, productId={}", actorId, store.getStoreId(), product.getProductId());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeHidden(Long actorId, UserRole actorRole, UUID productId, ProductHiddenUpdateRequest request) {
        Product product = getProductOrThrow(productId);
        Store store = getStoreOrThrow(product.getStoreId());
        validateProductWritePermission(actorId, actorRole, store);

        product.changeHidden(request.hidden());

        log.info("상품 숨김 상태 변경 완료 - actorId={}, storeId={}, productId={}, hidden={}",
                actorId, store.getStoreId(), product.getProductId(), request.hidden());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeSoldOut(Long actorId, UserRole actorRole, UUID productId, ProductSoldOutUpdateRequest request) {
        Product product = getProductOrThrow(productId);
        Store store = getStoreOrThrow(product.getStoreId());
        validateProductWritePermission(actorId, actorRole, store);

        product.changeSoldOut(request.soldOut());

        log.info("상품 숨김 상태 변경 완료 - actorId={}, storeId={}, productId={}, soldOUt={}",
                actorId, store.getStoreId(), product.getProductId(), request.soldOut());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse delete(Long actorId, UserRole actorRole, UUID productId) {
        Product product = getProductOrThrow(productId);
        Store store = getStoreOrThrow(product.getStoreId());
        validateProductWritePermission(actorId, actorRole, store);

        product.softDelete(actorId);

        log.info("상품 삭제 완료 -  - actorId={}, storeId={}, productId={}",
                actorId, store.getStoreId(), product.getProductId());
        return ProductResponse.from(product);
    }


    private Store getStoreOrThrow(UUID storeId) {
        // TODO: Store 도메인 예외 구현 즉시 해당 예외로 변경
        return storeRepository.findByStoreId(storeId)
                .orElseThrow(IllegalArgumentException::new);
    }
    private Product getProductOrThrow(UUID productId) {
        // 존재하는 상품인지 확인
        return productRepository.findByProductId(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private void validateProductWritePermission(Long actorId, UserRole actorRole, Store store) {
        // 특정 상품을 수정할 수 있는 권한인지 확인: 해당 상품의 소유주 혹은 관리자
        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            return;
        }
        if (actorRole == UserRole.OWNER && store.getUserId().equals(actorId)) {
            return;
        }
        throw new ProductForbiddenException();
    }

    private void validateDuplicateProductName(UUID storeId, String productName) {
        // e
        if (productRepository.existsByStoreIdAndProductName(storeId, productName)) {
            throw new DuplicateProductNameException();
        }
    }

    private void validateDuplicateProductNameOnUpdate(Product product,String productName) {
        boolean nameChanged = !product.getProductName().equals(productName);
        if (nameChanged && productRepository.existsByStoreIdAndProductName(product.getStoreId(), productName)) {
            throw new DuplicateProductNameException();
        }
    }

    private Product createProduct(
            UUID storeId,
            ProductCreateRequest request,
            String description,
            DescriptionSource descriptionSource
    ) {
        return Product.create(
                storeId,
                request.productName(),
                description,
                descriptionSource,
                request.price(),
                request.displayOrder()
        );
    }

    private int normalizePageSize(int size) {
        return (size == 10 || size == 30 || size == 50) ? size : 10;
    }
    private Sort normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";

        if (!property.equals("createdAt")
                && !property.equals("price")
                && !property.equals("displayOrder")
                && !property.equals("productName")
        ) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Sort.Direction sortDirection = direction.equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(sortDirection, property);
    }

    private Page<Product> getProductPage(
            UUID storeId,
            String keyword,
            boolean canViewHidden,
            Pageable pageable
    ) {
        if (keyword == null) {
            return canViewHidden
                    ? productRepository.findByStoreId(storeId, pageable)
                    : productRepository.findByStoreIdAndIsHiddenFalse(storeId, pageable);
        }

        return canViewHidden
                ? productRepository.findByStoreIdAndProductNameContainingIgnoreCase(storeId, keyword, pageable)
                : productRepository.findByStoreIdAndIsHiddenFalseAndProductNameContainingIgnoreCase(storeId, keyword, pageable);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String resolveDescription(Long actorId, ProductCreateRequest request) {
        if (request.shouldGenerateDescription()) {
            return aiDescriptionService.generateDescription(
                    actorId,
                    request.productName(),
                    request.price(),
                    request.aiPromptText()
            );
        }
        return request.description();
    }

    private DescriptionSource resolveDescriptionSource(ProductCreateRequest request, String description) {
        if (description == null) {
            return null;
        }

        return request.shouldGenerateDescription()
                ? DescriptionSource.AI_GENERATED
                : DescriptionSource.MANUAL;
    }
}
