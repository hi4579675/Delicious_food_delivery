package com.sparta.delivery.store.application;

import com.sparta.delivery.store.domain.entity.StoreCategory;
import com.sparta.delivery.store.domain.exception.DuplicateCategoryNameException;
import com.sparta.delivery.store.domain.exception.DuplicateCategorySortOrderException;
import com.sparta.delivery.store.domain.exception.StoreCategoryNotFoundException;
import com.sparta.delivery.store.domain.repository.StoreCategoryRepository;
import com.sparta.delivery.store.presentation.dto.StoreCategoryCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreCategoryResponse;
import com.sparta.delivery.store.presentation.dto.StoreCategoryUpdateRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreCategoryService {

    private final StoreCategoryRepository storeCategoryRepository;

    /** 가게 카테고리를 생성한다. */
    @Transactional
    public StoreCategoryResponse createCategory(StoreCategoryCreateRequest request) {
        String normalizedCategoryName = normalize(request.categoryName());

        validateDuplicateCategoryName(normalizedCategoryName);

        Integer nextSortOrder = getNextSortOrder();

        StoreCategory category = StoreCategory.create(
                normalizedCategoryName,
                request.description(),
                nextSortOrder,
                request.isActive()
        );

        StoreCategory savedCategory = storeCategoryRepository.save(category);

        log.info("가게 카테고리 생성 완료 - categoryId={}, categoryName={}, sortOrder={}, 활성여부={}",
                savedCategory.getCategoryId(),
                savedCategory.getCategoryName(),
                savedCategory.getSortOrder(),
                savedCategory.getIsActive());

        return StoreCategoryResponse.from(savedCategory);
    }

    /** 가게 카테고리 목록을 조회한다. */
    public List<StoreCategoryResponse> getCategories() {
        return storeCategoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(StoreCategoryResponse::from)
                .toList();
    }

    /** 활성화된 가게 카테고리 목록을 조회한다. */
    public List<StoreCategoryResponse> getActiveCategories() {
        return storeCategoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(StoreCategoryResponse::from)
                .toList();
    }

    /** 가게 카테고리를 단건 조회한다. */
    public StoreCategoryResponse getCategory(UUID categoryId) {
        return StoreCategoryResponse.from(getCategoryOrThrow(categoryId));
    }

    /** 가게 카테고리 정보를 수정한다. */
    @Transactional
    public StoreCategoryResponse updateCategory(UUID categoryId, StoreCategoryUpdateRequest request) {
        StoreCategory category = getCategoryOrThrow(categoryId);
        String normalizedCategoryName = normalize(request.categoryName());

        validateDuplicateCategoryName(category, normalizedCategoryName);
        validateDuplicateSortOrder(category, request.sortOrder());

        category.update(
                normalizedCategoryName,
                request.description(),
                request.sortOrder(),
                request.isActive()
        );

        log.info("가게 카테고리 수정 완료 - categoryId={}, categoryName={}, sortOrder={}, 활성여부={}",
                categoryId,
                category.getCategoryName(),
                category.getSortOrder(),
                category.getIsActive());

        return StoreCategoryResponse.from(category);
    }

    /** 가게 카테고리를 삭제한다. */
    @Transactional
    public void deleteCategory(UUID categoryId, Long currentUserId) {
        StoreCategory category = getCategoryOrThrow(categoryId);
        category.softDelete(currentUserId);

        log.info("가게 카테고리 삭제 완료 - actorId={}, categoryId={}, categoryName={}, sortOrder={}",
                currentUserId,
                categoryId,
                category.getCategoryName(),
                category.getSortOrder());
    }

    private StoreCategory getCategoryOrThrow(UUID categoryId) {
        return storeCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(StoreCategoryNotFoundException::new);
    }

    private Integer getNextSortOrder() {
        return storeCategoryRepository.findTopByOrderBySortOrderDesc()
                .map(category -> category.getSortOrder() + 1)
                .orElse(1);
    }

    private void validateDuplicateCategoryName(String categoryName) {
        if (storeCategoryRepository.existsByCategoryName(categoryName)) {
            throw new DuplicateCategoryNameException();
        }
    }

    private void validateDuplicateCategoryName(StoreCategory category, String categoryName) {
        if (!category.getCategoryName().equals(categoryName)
                && storeCategoryRepository.existsByCategoryName(categoryName)) {
            throw new DuplicateCategoryNameException();
        }
    }

    private void validateDuplicateSortOrder(StoreCategory category, Integer sortOrder) {
        if (!category.getSortOrder().equals(sortOrder)
                && storeCategoryRepository.existsBySortOrder(sortOrder)) {
            throw new DuplicateCategorySortOrderException();
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
