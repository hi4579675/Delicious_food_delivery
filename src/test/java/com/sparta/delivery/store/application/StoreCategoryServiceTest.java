package com.sparta.delivery.store.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreCategoryServiceTest {

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @InjectMocks
    private StoreCategoryService storeCategoryService;

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategoryTest {

        @Test
        @DisplayName("정상적으로 카테고리를 생성하고 sortOrder를 자동 부여한다")
        void createCategory_success() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategoryCreateRequest request = new StoreCategoryCreateRequest(
                    "치킨",
                    "치킨 카테고리",
                    true
            );

            StoreCategory lastCategory = createCategory(UUID.randomUUID(), "피자", 3, true);

            given(storeCategoryRepository.existsByCategoryNameIncludingDeleted("치킨")).willReturn(false);
            given(storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1)))
                    .willReturn(List.of(lastCategory));
            given(storeCategoryRepository.saveAndFlush(any(StoreCategory.class))).willAnswer(invocation -> {
                StoreCategory savedCategory = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedCategory, "categoryId", categoryId);
                return savedCategory;
            });

            // when
            var response = storeCategoryService.createCategory(request);

            // then
            assertThat(response.categoryId()).isEqualTo(categoryId);
            assertThat(response.categoryName()).isEqualTo("치킨");
            assertThat(response.sortOrder()).isEqualTo(4);
            assertThat(response.isActive()).isTrue();

            then(storeCategoryRepository).should().existsByCategoryNameIncludingDeleted("치킨");
            then(storeCategoryRepository).should().findAllByOrderBySortOrderDesc(PageRequest.of(0, 1));
            then(storeCategoryRepository).should().saveAndFlush(any(StoreCategory.class));
        }

        @Test
        @DisplayName("첫 카테고리 생성이면 sortOrder를 1로 부여한다")
        void createCategory_success_whenFirstCategory() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategoryCreateRequest request = new StoreCategoryCreateRequest(
                    "치킨",
                    "치킨 카테고리",
                    true
            );

            given(storeCategoryRepository.existsByCategoryNameIncludingDeleted("치킨")).willReturn(false);
            given(storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1)))
                    .willReturn(List.of());
            given(storeCategoryRepository.saveAndFlush(any(StoreCategory.class))).willAnswer(invocation -> {
                StoreCategory savedCategory = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedCategory, "categoryId", categoryId);
                return savedCategory;
            });

            // when
            var response = storeCategoryService.createCategory(request);

            // then
            assertThat(response.sortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("카테고리명이 중복되면 예외가 발생한다")
        void createCategory_fail_whenDuplicateName() {
            // given
            StoreCategoryCreateRequest request = new StoreCategoryCreateRequest(
                    "치킨",
                    "치킨 카테고리",
                    true
            );

            given(storeCategoryRepository.existsByCategoryNameIncludingDeleted("치킨")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> storeCategoryService.createCategory(request))
                    .isInstanceOf(DuplicateCategoryNameException.class);

            then(storeCategoryRepository).should(never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("카테고리 조회")
    class GetCategoryTest {

        @Test
        @DisplayName("카테고리를 단건 조회한다")
        void getCategory_success() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategory category = createCategory(categoryId, "치킨", 1, true);

            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));

            // when
            var response = storeCategoryService.getCategory(categoryId);

            // then
            assertThat(response.categoryId()).isEqualTo(categoryId);
            assertThat(response.categoryName()).isEqualTo("치킨");
        }

        @Test
        @DisplayName("카테고리가 없으면 예외가 발생한다")
        void getCategory_fail_whenNotFound() {
            // given
            UUID categoryId = UUID.randomUUID();
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeCategoryService.getCategory(categoryId))
                    .isInstanceOf(StoreCategoryNotFoundException.class);
        }

        @Test
        @DisplayName("비활성 카테고리는 공개 단건 조회에서 숨긴다")
        void getCategory_fail_whenInactive() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategory inactiveCategory = createCategory(categoryId, "치킨", 1, false);
            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(inactiveCategory));

            // when & then
            assertThatThrownBy(() -> storeCategoryService.getCategory(categoryId))
                    .isInstanceOf(StoreCategoryNotFoundException.class);
        }

        @Test
        @DisplayName("전체 카테고리 목록을 조회한다")
        void getCategories_success() {
            // given
            StoreCategory chicken = createCategory(UUID.randomUUID(), "치킨", 1, true);
            StoreCategory pizza = createCategory(UUID.randomUUID(), "피자", 2, true);
            Pageable pageable = PageRequest.of(0, 10);

            given(storeCategoryRepository.findAllByIsActiveTrue(pageable))
                    .willReturn(new PageImpl<>(List.of(chicken, pizza), pageable, 2));

            // when
            PageResponse<StoreCategoryResponse> responses = storeCategoryService.getCategories(pageable);

            // then
            assertThat(responses.content()).hasSize(2);
            assertThat(responses.content().get(0).categoryName()).isEqualTo("치킨");
            assertThat(responses.content().get(1).categoryName()).isEqualTo("피자");
            assertThat(responses.page()).isEqualTo(0);
            assertThat(responses.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("비활성 카테고리 목록을 조회한다")
        void getInactiveCategories_success() {
            // given
            StoreCategory chicken = createCategory(UUID.randomUUID(), "치킨", 1, false);
            Pageable pageable = PageRequest.of(0, 10);

            given(storeCategoryRepository.findAllByIsActiveFalse(pageable))
                    .willReturn(new PageImpl<>(List.of(chicken), pageable, 1));

            // when
            PageResponse<StoreCategoryResponse> responses = storeCategoryService.getInactiveCategories(pageable);

            // then
            assertThat(responses.content()).hasSize(1);
            assertThat(responses.content().get(0).categoryName()).isEqualTo("치킨");
            assertThat(responses.content().get(0).isActive()).isFalse();
        }

        @Test
        @DisplayName("전체 카테고리 목록을 조회한다")
        void getAllCategories_success() {
            // given
            StoreCategory chicken = createCategory(UUID.randomUUID(), "치킨", 1, true);
            StoreCategory pizza = createCategory(UUID.randomUUID(), "피자", 2, false);
            Pageable pageable = PageRequest.of(0, 10);

            given(storeCategoryRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(chicken, pizza), pageable, 2));

            // when
            PageResponse<StoreCategoryResponse> responses = storeCategoryService.getAllCategories(pageable);

            // then
            assertThat(responses.content()).hasSize(2);
            assertThat(responses.content())
                    .extracting(StoreCategoryResponse::isActive)
                    .containsExactly(true, false);
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategoryTest {

        @Test
        @DisplayName("정상적으로 카테고리를 수정한다")
        void updateCategory_success() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategory category = createCategory(categoryId, "치킨", 1, true);

            StoreCategoryUpdateRequest request = new StoreCategoryUpdateRequest(
                    "치킨 수정",
                    "설명 수정",
                    2,
                    false
            );

            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));
            given(storeCategoryRepository.existsByCategoryNameIncludingDeleted("치킨 수정")).willReturn(false);
            given(storeCategoryRepository.existsBySortOrderIncludingDeleted(2)).willReturn(false);
            given(storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1)))
                    .willReturn(List.of(category));

            // when
            var response = storeCategoryService.updateCategory(categoryId, request);

            // then
            assertThat(response.categoryName()).isEqualTo("치킨 수정");
            assertThat(response.description()).isEqualTo("설명 수정");
            assertThat(response.sortOrder()).isEqualTo(2);
            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("카테고리명이 중복되면 예외가 발생한다")
        void updateCategory_fail_whenDuplicateName() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategory category = createCategory(categoryId, "치킨", 1, true);

            StoreCategoryUpdateRequest request = new StoreCategoryUpdateRequest(
                    "피자",
                    "설명 수정",
                    2,
                    true
            );

            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));
            given(storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1)))
                    .willReturn(List.of(category));
            given(storeCategoryRepository.existsByCategoryNameIncludingDeleted("피자")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> storeCategoryService.updateCategory(categoryId, request))
                    .isInstanceOf(DuplicateCategoryNameException.class);
        }

        @Test
        @DisplayName("정렬 순서가 중복되면 예외가 발생한다")
        void updateCategory_fail_whenDuplicateSortOrder() {
            // given
            UUID categoryId = UUID.randomUUID();
            StoreCategory category = createCategory(categoryId, "치킨", 1, true);

            StoreCategoryUpdateRequest request = new StoreCategoryUpdateRequest(
                    "치킨",
                    "설명 수정",
                    2,
                    true
            );

            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));
            given(storeCategoryRepository.findAllByOrderBySortOrderDesc(PageRequest.of(0, 1)))
                    .willReturn(List.of(category));
            given(storeCategoryRepository.existsBySortOrderIncludingDeleted(2)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> storeCategoryService.updateCategory(categoryId, request))
                    .isInstanceOf(DuplicateCategorySortOrderException.class);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class DeleteCategoryTest {

        @Test
        @DisplayName("카테고리를 soft delete 처리한다")
        void deleteCategory_success() {
            // given
            UUID categoryId = UUID.randomUUID();
            Long currentUserId = 1L;
            StoreCategory category = createCategory(categoryId, "치킨", 1, true);

            given(storeCategoryRepository.findByCategoryId(categoryId)).willReturn(Optional.of(category));

            // when
            storeCategoryService.deleteCategory(categoryId, currentUserId);

            // then
            assertThat(category.isDeleted()).isTrue();
            assertThat(category.getDeletedBy()).isEqualTo(currentUserId);
        }
    }

    private StoreCategory createCategory(UUID categoryId, String categoryName, Integer sortOrder, Boolean isActive) {
        StoreCategory category = StoreCategory.create(
                categoryName,
                categoryName + " 설명",
                sortOrder,
                isActive
        );
        ReflectionTestUtils.setField(category, "categoryId", categoryId);
        return category;
    }
}
