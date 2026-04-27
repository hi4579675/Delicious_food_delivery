package com.sparta.delivery.store.application;

import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.region.domain.entity.Region;
import com.sparta.delivery.region.domain.repository.RegionRepository;
import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.domain.entity.StoreCategory;
import com.sparta.delivery.store.domain.exception.InactiveStoreCategoryException;
import com.sparta.delivery.store.domain.exception.InactiveStoreRegionException;
import com.sparta.delivery.store.domain.exception.InvalidStoreRegionDepthException;
import com.sparta.delivery.store.domain.exception.StoreCategoryNotFoundException;
import com.sparta.delivery.store.domain.exception.StoreForbiddenException;
import com.sparta.delivery.store.domain.exception.StoreNotFoundException;
import com.sparta.delivery.store.domain.exception.StoreRegionNotFoundException;
import com.sparta.delivery.store.domain.repository.StoreCategoryRepository;
import com.sparta.delivery.store.domain.repository.StoreRepository;
import com.sparta.delivery.store.presentation.dto.StoreCreateRequest;
import com.sparta.delivery.store.presentation.dto.StoreResponse;
import com.sparta.delivery.store.presentation.dto.StoreSearchCondition;
import com.sparta.delivery.store.presentation.dto.StoreUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final RegionRepository regionRepository;

    /** 가게를 생성한다. */
    @Transactional
    public StoreResponse createStore(Long userId, StoreCreateRequest request) {
        Region region = validateRegion(request.regionId());
        StoreCategory category = validateCategory(request.categoryId());

        Store store = Store.create(
                region.getRegionId(),
                category.getCategoryId(),
                userId,
                normalize(request.storeName()),
                normalize(request.description()),
                normalize(request.address()),
                normalize(request.addressDetail()),
                normalize(request.phoneNumber()),
                request.minOrderAmount(),
                request.isOpen(),
                request.isActive(),
                BigDecimal.ZERO,
                0
        );

        Store savedStore = storeRepository.save(store);

        log.info("가게 생성 완료 - userId={}, storeId={}, storeName={}, regionId={}, categoryId={}",
                userId,
                savedStore.getStoreId(),
                savedStore.getStoreName(),
                savedStore.getRegionId(),
                savedStore.getCategoryId());

        return StoreResponse.from(savedStore);
    }

    /** 조건에 따라 가게 목록을 조회한다. */
    public PageResponse<StoreResponse> searchStores(StoreSearchCondition condition, Pageable pageable) {
        StoreSearchCondition normalizedCondition = condition == null
                ? new StoreSearchCondition(null, null, null, null, null, null, null, null, null, null, null)
                : condition;

        Pageable normalizedPageable = Objects.requireNonNull(pageable, "pageable must not be null");

        Page<StoreResponse> page = storeRepository.searchStores(normalizedCondition, normalizedPageable)
                .map(StoreResponse::from);

        return PageResponse.from(page);
    }

    /** 가게를 단건 조회한다. */
    public StoreResponse getStore(UUID storeId) {
        return StoreResponse.from(getStoreOrThrow(storeId));
    }

    /** 가게 정보를 수정한다. */
    @Transactional
    public StoreResponse updateStore(
            UUID storeId,
            Long actorId,
            UserRole actorRole,
            StoreUpdateRequest request
    ) {
        Store store = getStoreOrThrow(storeId);
        validateStoreAccess(store, actorId, actorRole);

        Region region = validateRegion(request.regionId());
        StoreCategory category = validateCategory(request.categoryId());

        store.update(
                region.getRegionId(),
                category.getCategoryId(),
                normalize(request.storeName()),
                normalize(request.description()),
                normalize(request.address()),
                normalize(request.addressDetail()),
                normalize(request.phoneNumber()),
                request.minOrderAmount(),
                request.isOpen(),
                request.isActive()
        );

        log.info("가게 수정 완료 - actorId={}, actorRole={}, storeId={}, storeName={}, regionId={}, categoryId={}",
                actorId,
                actorRole,
                storeId,
                store.getStoreName(),
                store.getRegionId(),
                store.getCategoryId());

        return StoreResponse.from(store);
    }

    /** 가게를 삭제한다. */
    @Transactional
    public void deleteStore(UUID storeId, Long actorId, UserRole actorRole) {
        Store store = getStoreOrThrow(storeId);
        validateStoreAccess(store, actorId, actorRole);

        store.softDelete(actorId);

        log.info("가게 삭제 완료 - actorId={}, actorRole={}, storeId={}, storeName={}",
                actorId,
                actorRole,
                storeId,
                store.getStoreName());
    }

    private Store getStoreOrThrow(UUID storeId) {
        return storeRepository.findByStoreId(storeId)
                .orElseThrow(StoreNotFoundException::new);
    }

    private void validateStoreAccess(Store store, Long actorId, UserRole actorRole) {
        if (actorRole == UserRole.MANAGER || actorRole == UserRole.MASTER) {
            return;
        }

        if (actorRole == UserRole.OWNER && store.getUserId().equals(actorId)) {
            return;
        }

        throw new StoreForbiddenException();
    }

    private Region validateRegion(UUID regionId) {
        Region region = regionRepository.findByRegionId(regionId)
                .orElseThrow(StoreRegionNotFoundException::new);

        if (!region.getIsActive()) {
            throw new InactiveStoreRegionException();
        }

        if (region.getDepth() != 3) {
            throw new InvalidStoreRegionDepthException();
        }

        return region;
    }

    private StoreCategory validateCategory(UUID categoryId) {
        StoreCategory category = storeCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(StoreCategoryNotFoundException::new);

        if (!category.getIsActive()) {
            throw new InactiveStoreCategoryException();
        }

        return category;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
