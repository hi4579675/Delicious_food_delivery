package com.sparta.delivery.store.application;

import com.sparta.delivery.common.response.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            StoreCategory category = StoreCategory.create(
                    normalizedCategoryName,
                    request.description(),
                    nextSortOrder,
                    request.isActive()
            );

            StoreCategory savedCategory = storeCategoryRepository.saveAndFlush(category);

            log.info("가게 카테고리 생성 완료 - categoryId={}, categoryName={}, sortOrder={}, 활성여부={}",
                    savedCategory.getCategoryId(),
                    savedCategory.getCategoryName(),
                    savedCategory.getSortOrder(),
                    savedCategory.getIsActive());

            return StoreCategoryResponse.from(savedCategory);
        } catch (DataIntegrityViolationException e) {
            if (storeCategoryRepository.existsByCategoryNameIncludingDeleted(normalizedCategoryName)) {
                throw new DuplicateCategoryNameException();
            }

            if (storeCategoryRepository.existsBySortOrderIncludingDeleted(nextSortOrder)) {
                throw new DuplicateCategorySortOrderException();
            }

            throw e;
        }
    }

    /** 활성화된 가게 카테고리 목록을 조회한다. */
    public PageResponse<StoreCategoryResponse> getCategories(Pageable pageable) {
        Page<StoreCategoryResponse> page = storeCategoryRepository.findAllByIsActiveTrue(pageable)
                .map(StoreCategoryResponse::from);

        return PageResponse.from(page);
    }

    /** 비활성화된 가게 카테고리 목록을 조회한다. */
    public PageResponse<StoreCategoryResponse> getInactiveCategories(Pageable pageable) {
        Page<StoreCategoryResponse> page = storeCategoryRepository.findAllByIsActiveFalse(pageable)
                .map(StoreCategoryResponse::from);

        return PageResponse.from(page);
    }

    /** 전체 가게 카테고리 목록을 조회한다. */
    public PageResponse<StoreCategoryResponse> getAllCategories(Pageable pageable) {
        Page<StoreCategoryResponse> page = storeCategoryRepository.findAll(pageable)
                .map(StoreCategoryResponse::from);

        return PageResponse.from(page);
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

        acquireSortOrderLock();
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
        StoreCategory lastCategory = getLastCategoryWithLock();
        return lastCategory == null ? 1 : lastCategory.getSortOrder() + 1;
    }

    private void validateDuplicateCategoryName(String categoryName) {
        if (storeCategoryRepository.existsByCategoryNameIncludingDeleted(categoryName)) {
            throw new DuplicateCategoryNameException();
        }
    }

    private void validateDuplicateCategoryName(StoreCategory category, String categoryName) {
        if (!category.getCategoryName().equals(categoryName)
                && storeCategoryRepository.existsByCategoryNameIncludingDeleted(categoryName)) {
            throw new DuplicateCategoryNameException();
        }
    }

    private void validateDuplicateSortOrder(StoreCategory category, Integer sortOrder) {
        if (!category.getSortOrder().equals(sortOrder)
                && storeCategoryRepository.existsBySortOrderIncludingDeleted(sortOrder)) {
            throw new DuplicateCategorySortOrderException();
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void acquireSortOrderLock() {
        getLastCategoryWithLock();
    }

    private StoreCategory getLastCategoryWithLock() {
        return storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
