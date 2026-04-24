package com.sparta.delivery.store.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

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
import com.sparta.delivery.store.presentation.dto.StoreUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private StoreService storeService;

    @Nested
    @DisplayName("가게 생성")
    class CreateStoreTest {

        @Test
        @DisplayName("정상적으로 가게를 생성한다")
        void createStore_success() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            Region region = createRegion(regionId, 3, true);
            StoreCategory category = createCategory(categoryId, true);

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));
            given(storeRepository.save(any(Store.class))).willAnswer(invocation -> {
                Store savedStore = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedStore, "storeId", storeId);
                return savedStore;
            });

            // when
            var response = storeService.createStore(userId, request);

            // then
            assertThat(response.storeId()).isEqualTo(storeId);
            assertThat(response.regionId()).isEqualTo(regionId);
            assertThat(response.categoryId()).isEqualTo(categoryId);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.storeName()).isEqualTo("왕조치킨");
            assertThat(response.reviewCount()).isEqualTo(0);

            then(regionRepository).should().findByRegionId(regionId);
            then(storeCategoryRepository).should().findByCategoryId(categoryId);
            then(storeRepository).should().save(any(Store.class));
        }

        @Test
        @DisplayName("지역이 없으면 예외가 발생한다")
        void createStore_fail_whenRegionNotFound() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.createStore(userId, request))
                    .isInstanceOf(StoreRegionNotFoundException.class);

            then(storeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("지역이 비활성이면 예외가 발생한다")
        void createStore_fail_whenRegionInactive() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            Region inactiveRegion = createRegion(regionId, 3, false);

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(inactiveRegion));

            // when & then
            assertThatThrownBy(() -> storeService.createStore(userId, request))
                    .isInstanceOf(InactiveStoreRegionException.class);

            then(storeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("지역 depth가 3이 아니면 예외가 발생한다")
        void createStore_fail_whenRegionDepthInvalid() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            Region invalidDepthRegion = createRegion(regionId, 2, true);

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(invalidDepthRegion));

            // when & then
            assertThatThrownBy(() -> storeService.createStore(userId, request))
                    .isInstanceOf(InvalidStoreRegionDepthException.class);

            then(storeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("카테고리가 없으면 예외가 발생한다")
        void createStore_fail_whenCategoryNotFound() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            Region region = createRegion(regionId, 3, true);

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.createStore(userId, request))
                    .isInstanceOf(StoreCategoryNotFoundException.class);

            then(storeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("카테고리가 비활성이면 예외가 발생한다")
        void createStore_fail_whenCategoryInactive() {
            // given
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            StoreCreateRequest request = new StoreCreateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨",
                    "바삭한 치킨 전문점",
                    "서울시 종로구 1번지",
                    "101호",
                    "02-1234-5678",
                    15000,
                    true,
                    true
            );

            Region region = createRegion(regionId, 3, true);
            StoreCategory inactiveCategory = createCategory(categoryId, false);

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(inactiveCategory));

            // when & then
            assertThatThrownBy(() -> storeService.createStore(userId, request))
                    .isInstanceOf(InactiveStoreCategoryException.class);

            then(storeRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("가게 목록 조회")
    class GetStoresTest {

        @Test
        @DisplayName("조건이 없으면 전체 가게 목록을 조회한다")
        void getStores_success_withoutCondition() {
            // given
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            given(storeRepository.findAll()).willReturn(List.of(store));

            // when
            var responses = storeService.getStores(null, null);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).storeName()).isEqualTo("왕조치킨");

            then(storeRepository).should().findAll();
        }

        @Test
        @DisplayName("지역 조건으로 가게 목록을 조회한다")
        void getStores_success_byRegion() {
            // given
            UUID regionId = UUID.randomUUID();
            Store store = createStoreEntity(regionId, UUID.randomUUID(), 1L);

            given(storeRepository.findByRegionId(regionId)).willReturn(List.of(store));

            // when
            var responses = storeService.getStores(regionId, null);

            // then
            assertThat(responses).hasSize(1);
            then(storeRepository).should().findByRegionId(regionId);
        }

        @Test
        @DisplayName("카테고리 조건으로 가게 목록을 조회한다")
        void getStores_success_byCategory() {
            // given
            UUID categoryId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), categoryId, 1L);

            given(storeRepository.findByCategoryId(categoryId)).willReturn(List.of(store));

            // when
            var responses = storeService.getStores(null, categoryId);

            // then
            assertThat(responses).hasSize(1);
            then(storeRepository).should().findByCategoryId(categoryId);
        }

        @Test
        @DisplayName("지역과 카테고리 조건으로 가게 목록을 조회한다")
        void getStores_success_byRegionAndCategory() {
            // given
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Store store = createStoreEntity(regionId, categoryId, 1L);

            given(storeRepository.findByRegionIdAndCategoryId(regionId, categoryId))
                    .willReturn(List.of(store));

            // when
            var responses = storeService.getStores(regionId, categoryId);

            // then
            assertThat(responses).hasSize(1);
            then(storeRepository).should().findByRegionIdAndCategoryId(regionId, categoryId);
        }
    }

    @Nested
    @DisplayName("가게 단건 조회")
    class GetStoreTest {

        @Test
        @DisplayName("가게를 단건 조회한다")
        void getStore_success() {
            // given
            UUID storeId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when
            var response = storeService.getStore(storeId);

            // then
            assertThat(response.storeId()).isEqualTo(storeId);
            assertThat(response.storeName()).isEqualTo("왕조치킨");
        }

        @Test
        @DisplayName("가게가 없으면 예외가 발생한다")
        void getStore_fail_whenNotFound() {
            // given
            UUID storeId = UUID.randomUUID();
            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.getStore(storeId))
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("가게 수정")
    class UpdateStoreTest {

        @Test
        @DisplayName("본인 가게를 정상적으로 수정한다")
        void updateStore_success() {
            // given
            UUID storeId = UUID.randomUUID();
            Long userId = 1L;
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Store store = createStoreEntity(regionId, categoryId, userId);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            Region region = createRegion(regionId, 3, true);
            StoreCategory category = createCategory(categoryId, true);

            StoreUpdateRequest request = new StoreUpdateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));

            // when
            var response = storeService.updateStore(storeId, userId, UserRole.OWNER, request);

            // then
            assertThat(response.storeName()).isEqualTo("왕조치킨 수정");
            assertThat(response.minOrderAmount()).isEqualTo(20000);
            assertThat(response.isOpen()).isFalse();
        }

        @Test
        @DisplayName("본인 가게가 아니면 예외가 발생한다")
        void updateStore_fail_whenForbidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            StoreUpdateRequest request = new StoreUpdateRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, 2L, UserRole.OWNER, request))
                    .isInstanceOf(StoreForbiddenException.class);
        }

        @Test
        @DisplayName("매니저는 다른 가게도 수정할 수 있다")
        void updateStore_success_whenManager() {
            // given
            UUID storeId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Store store = createStoreEntity(regionId, categoryId, 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            Region region = createRegion(regionId, 3, true);
            StoreCategory category = createCategory(categoryId, true);

            StoreUpdateRequest request = new StoreUpdateRequest(
                    regionId,
                    categoryId,
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));
            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));

            // when
            var response = storeService.updateStore(storeId, 99L, UserRole.MANAGER, request);

            // then
            assertThat(response.storeName()).isEqualTo("왕조치킨 수정");
            assertThat(response.minOrderAmount()).isEqualTo(20000);
        }

        @Test
        @DisplayName("고객은 가게를 수정할 수 없다")
        void updateStore_fail_whenCustomer() {
            // given
            UUID storeId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            StoreUpdateRequest request = new StoreUpdateRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "왕조치킨 수정",
                    "설명 수정",
                    "주소 수정",
                    "상세주소 수정",
                    "02-9999-8888",
                    20000,
                    false,
                    true
            );

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, 1L, UserRole.CUSTOMER, request))
                    .isInstanceOf(StoreForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("가게 삭제")
    class DeleteStoreTest {

        @Test
        @DisplayName("본인 가게를 soft delete 처리한다")
        void deleteStore_success() {
            // given
            UUID storeId = UUID.randomUUID();
            Long userId = 1L;
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), userId);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when
            storeService.deleteStore(storeId, userId, UserRole.OWNER);

            // then
            assertThat(store.isDeleted()).isTrue();
            assertThat(store.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("본인 가게가 아니면 삭제할 수 없다")
        void deleteStore_fail_whenForbidden() {
            // given
            UUID storeId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(storeId, 2L, UserRole.OWNER))
                    .isInstanceOf(StoreForbiddenException.class);
        }

        @Test
        @DisplayName("매니저는 다른 가게도 삭제할 수 있다")
        void deleteStore_success_whenManager() {
            // given
            UUID storeId = UUID.randomUUID();
            Long actorId = 99L;
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when
            storeService.deleteStore(storeId, actorId, UserRole.MANAGER);

            // then
            assertThat(store.isDeleted()).isTrue();
            assertThat(store.getDeletedBy()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("고객은 가게를 삭제할 수 없다")
        void deleteStore_fail_whenCustomer() {
            // given
            UUID storeId = UUID.randomUUID();
            Store store = createStoreEntity(UUID.randomUUID(), UUID.randomUUID(), 1L);
            ReflectionTestUtils.setField(store, "storeId", storeId);

            given(storeRepository.findByStoreId(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(storeId, 1L, UserRole.CUSTOMER))
                    .isInstanceOf(StoreForbiddenException.class);
        }
    }

    private Region createRegion(UUID regionId, Integer depth, Boolean isActive) {
        Region region = Region.create("1111012300", "청운효자동", UUID.randomUUID(), depth, isActive);
        ReflectionTestUtils.setField(region, "regionId", regionId);
        return region;
    }

    private StoreCategory createCategory(UUID categoryId, Boolean isActive) {
        StoreCategory category = StoreCategory.create("치킨", "치킨 카테고리", 1, isActive);
        ReflectionTestUtils.setField(category, "categoryId", categoryId);
        return category;
    }

    private Store createStoreEntity(UUID regionId, UUID categoryId, Long userId) {
        return Store.create(
                regionId,
                categoryId,
                userId,
                "왕조치킨",
                "설명",
                "주소",
                "상세주소",
                "02-1111-2222",
                15000,
                true,
                true,
                BigDecimal.ZERO,
                0
        );
    }
}
