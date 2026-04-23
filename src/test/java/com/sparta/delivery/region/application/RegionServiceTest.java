package com.sparta.delivery.region.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.sparta.delivery.region.domain.entity.Region;
import com.sparta.delivery.region.domain.exception.DuplicateRegionCodeException;
import com.sparta.delivery.region.domain.exception.InvalidParentRegionException;
import com.sparta.delivery.region.domain.exception.InvalidRegionDepthException;
import com.sparta.delivery.region.domain.exception.RegionHasChildrenException;
import com.sparta.delivery.region.domain.exception.RegionNotFoundException;
import com.sparta.delivery.region.domain.repository.RegionRepository;
import com.sparta.delivery.region.presentation.dto.RegionCreateRequest;
import com.sparta.delivery.region.presentation.dto.RegionUpdateRequest;
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

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    @Nested
    @DisplayName("지역 생성")
    class CreateRegionTest {

        @Test
        @DisplayName("정상적으로 루트 지역을 생성한다")
        void createRegion_success_root() {
            // given
            RegionCreateRequest request = new RegionCreateRequest(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionRepository.existsByRegionCode("1100000000")).willReturn(false);
            given(regionRepository.save(any(Region.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            var response = regionService.createRegion(request);

            // then
            assertThat(response.regionCode()).isEqualTo("1100000000");
            assertThat(response.regionName()).isEqualTo("서울특별시");
            assertThat(response.parentId()).isNull();
            assertThat(response.depth()).isEqualTo(1);
            assertThat(response.isActive()).isTrue();

            then(regionRepository).should().existsByRegionCode("1100000000");
            then(regionRepository).should().save(any(Region.class));
        }

        @Test
        @DisplayName("정상적으로 하위 지역을 생성한다")
        void createRegion_success_child() {
            // given
            UUID parentId = UUID.randomUUID();

            Region parent = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            RegionCreateRequest request = new RegionCreateRequest(
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            given(regionRepository.existsByRegionCode("1111000000")).willReturn(false);
            given(regionRepository.findByRegionId(parentId)).willReturn(Optional.of(parent));
            given(regionRepository.save(any(Region.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            var response = regionService.createRegion(request);

            // then
            assertThat(response.regionCode()).isEqualTo("1111000000");
            assertThat(response.regionName()).isEqualTo("종로구");
            assertThat(response.parentId()).isEqualTo(parentId);
            assertThat(response.depth()).isEqualTo(2);
            assertThat(response.isActive()).isTrue();

            then(regionRepository).should().existsByRegionCode("1111000000");
            then(regionRepository).should().findByRegionId(parentId);
            then(regionRepository).should().save(any(Region.class));
        }

        @Test
        @DisplayName("지역 코드가 중복되면 DuplicateRegionCodeException이 발생한다")
        void createRegion_fail_whenDuplicateCode() {
            // given
            RegionCreateRequest request = new RegionCreateRequest(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionRepository.existsByRegionCode("1100000000")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> regionService.createRegion(request))
                    .isInstanceOf(DuplicateRegionCodeException.class)
                    .hasMessageContaining("지역 코드");

            then(regionRepository).should().existsByRegionCode("1100000000");
            then(regionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("루트 지역의 depth가 1이 아니면 InvalidRegionDepthException이 발생한다")
        void createRegion_fail_whenRootDepthIsInvalid() {
            // given
            RegionCreateRequest request = new RegionCreateRequest(
                    "1100000000",
                    "서울특별시",
                    null,
                    2,
                    true
            );

            given(regionRepository.existsByRegionCode("1100000000")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> regionService.createRegion(request))
                    .isInstanceOf(InvalidRegionDepthException.class)
                    .hasMessageContaining("depth");

            then(regionRepository).should().existsByRegionCode("1100000000");
            then(regionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("부모 지역이 없으면 InvalidParentRegionException이 발생한다")
        void createRegion_fail_whenParentNotFound() {
            // given
            UUID parentId = UUID.randomUUID();

            RegionCreateRequest request = new RegionCreateRequest(
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            given(regionRepository.existsByRegionCode("1111000000")).willReturn(false);
            given(regionRepository.findByRegionId(parentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.createRegion(request))
                    .isInstanceOf(InvalidParentRegionException.class)
                    .hasMessageContaining("상위");

            then(regionRepository).should().existsByRegionCode("1111000000");
            then(regionRepository).should().findByRegionId(parentId);
            then(regionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("하위 지역의 depth가 부모 depth보다 1 크지 않으면 InvalidRegionDepthException이 발생한다")
        void createRegion_fail_whenChildDepthIsInvalid() {
            // given
            UUID parentId = UUID.randomUUID();

            Region parent = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            RegionCreateRequest request = new RegionCreateRequest(
                    "1111000000",
                    "종로구",
                    parentId,
                    3,
                    true
            );

            given(regionRepository.existsByRegionCode("1111000000")).willReturn(false);
            given(regionRepository.findByRegionId(parentId)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> regionService.createRegion(request))
                    .isInstanceOf(InvalidRegionDepthException.class)
                    .hasMessageContaining("depth");

            then(regionRepository).should().existsByRegionCode("1111000000");
            then(regionRepository).should().findByRegionId(parentId);
            then(regionRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("지역 조회")
    class GetRegionTest {

        @Test
        @DisplayName("지역 ID로 단건 조회한다")
        void getRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();

            Region region = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));

            // when
            var response = regionService.getRegion(regionId);

            // then
            assertThat(response.regionCode()).isEqualTo("1100000000");
            assertThat(response.regionName()).isEqualTo("서울특별시");

            then(regionRepository).should().findByRegionId(regionId);
        }

        @Test
        @DisplayName("지역이 없으면 RegionNotFoundException이 발생한다")
        void getRegion_fail_whenNotFound() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.getRegion(regionId))
                    .isInstanceOf(RegionNotFoundException.class)
                    .hasMessageContaining("지역");

            then(regionRepository).should().findByRegionId(regionId);
        }

        @Test
        @DisplayName("keyword가 없으면 전체 지역 목록을 조회한다")
        void searchRegions_success_withoutKeyword() {
            // given
            Region seoul = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            Region busan = Region.create(
                    "2600000000",
                    "부산광역시",
                    null,
                    1,
                    true
            );

            given(regionRepository.findAll()).willReturn(List.of(seoul, busan));

            // when
            var responses = regionService.searchRegions(null);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting("regionName")
                    .containsExactly("서울특별시", "부산광역시");

            then(regionRepository).should().findAll();
        }

        @Test
        @DisplayName("keyword가 있으면 지역명으로 검색한다")
        void searchRegions_success_withKeyword() {
            // given
            UUID parentId = UUID.randomUUID();

            Region jongno = Region.create(
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            given(regionRepository.findByRegionNameContaining("종로"))
                    .willReturn(List.of(jongno));

            // when
            var responses = regionService.searchRegions("종로");

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).regionName()).isEqualTo("종로구");
            assertThat(responses.get(0).parentId()).isEqualTo(parentId);

            then(regionRepository).should().findByRegionNameContaining("종로");
        }

        @Test
        @DisplayName("keyword 앞뒤 공백을 제거하고 지역명으로 검색한다")
        void searchRegions_success_withTrimmedKeyword() {
            // given
            Region jongno = Region.create(
                    "1111000000",
                    "종로구",
                    UUID.randomUUID(),
                    2,
                    true
            );

            given(regionRepository.findByRegionNameContaining("종로"))
                    .willReturn(List.of(jongno));

            // when
            var responses = regionService.searchRegions(" 종로 ");

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).regionName()).isEqualTo("종로구");

            then(regionRepository).should().findByRegionNameContaining("종로");
        }

        @Test
        @DisplayName("최상위 지역 목록을 조회한다")
        void getRootRegions_success() {
            // given
            Region seoul = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            Region busan = Region.create(
                    "2600000000",
                    "부산광역시",
                    null,
                    1,
                    true
            );

            given(regionRepository.findByParentIdIsNull()).willReturn(List.of(seoul, busan));

            // when
            var responses = regionService.getRootRegions();

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting("regionName")
                    .containsExactly("서울특별시", "부산광역시");

            then(regionRepository).should().findByParentIdIsNull();
        }

        @Test
        @DisplayName("하위 지역 목록을 조회한다")
        void getChildRegions_success() {
            // given
            UUID parentId = UUID.randomUUID();

            Region jongno = Region.create(
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            given(regionRepository.findByParentId(parentId)).willReturn(List.of(jongno));

            // when
            var responses = regionService.getChildRegions(parentId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).regionName()).isEqualTo("종로구");
            assertThat(responses.get(0).parentId()).isEqualTo(parentId);

            then(regionRepository).should().findByParentId(parentId);
        }
    }

    @Nested
    @DisplayName("지역 수정")
    class UpdateRegionTest {

        @Test
        @DisplayName("정상적으로 지역 정보를 수정한다")
        void updateRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();

            Region parent = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            Region region = Region.create(
                    "1111000000",
                    "종로구",
                    parentId,
                    2,
                    true
            );

            RegionUpdateRequest request = new RegionUpdateRequest(
                    "종로구 수정",
                    parentId,
                    2,
                    false
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(regionRepository.findByRegionId(parentId)).willReturn(Optional.of(parent));

            // when
            var response = regionService.updateRegion(regionId, request);

            // then
            assertThat(response.regionName()).isEqualTo("종로구 수정");
            assertThat(response.parentId()).isEqualTo(parentId);
            assertThat(response.depth()).isEqualTo(2);
            assertThat(response.isActive()).isFalse();

            then(regionRepository).should().findByRegionId(regionId);
            then(regionRepository).should().findByRegionId(parentId);
        }

        @Test
        @DisplayName("자기 자신을 부모로 지정하면 InvalidParentRegionException이 발생한다")
        void updateRegion_fail_whenParentIsSelf() {
            // given
            UUID regionId = UUID.randomUUID();

            Region region = Region.create(
                    "1111000000",
                    "종로구",
                    null,
                    1,
                    true
            );

            RegionUpdateRequest request = new RegionUpdateRequest(
                    "종로구 수정",
                    regionId,
                    2,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));

            // when & then
            assertThatThrownBy(() -> regionService.updateRegion(regionId, request))
                    .isInstanceOf(InvalidParentRegionException.class)
                    .hasMessageContaining("상위");

            then(regionRepository).should().findByRegionId(regionId);
        }

        @Test
        @DisplayName("수정 대상 지역이 없으면 RegionNotFoundException이 발생한다")
        void updateRegion_fail_whenRegionNotFound() {
            // given
            UUID regionId = UUID.randomUUID();

            RegionUpdateRequest request = new RegionUpdateRequest(
                    "종로구 수정",
                    null,
                    1,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.updateRegion(regionId, request))
                    .isInstanceOf(RegionNotFoundException.class)
                    .hasMessageContaining("지역");

            then(regionRepository).should().findByRegionId(regionId);
        }
    }

    @Nested
    @DisplayName("지역 삭제")
    class DeleteRegionTest {

        @Test
        @DisplayName("하위 지역이 없으면 soft delete 처리한다")
        void deleteRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();
            Long currentUserId = 1L;

            Region region = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(regionRepository.findByParentId(regionId)).willReturn(List.of());

            // when
            regionService.deleteRegion(regionId, currentUserId);

            // then
            assertThat(region.isDeleted()).isTrue();
            assertThat(region.getDeletedBy()).isEqualTo(currentUserId);

            then(regionRepository).should().findByRegionId(regionId);
            then(regionRepository).should().findByParentId(regionId);
        }

        @Test
        @DisplayName("하위 지역이 있으면 RegionHasChildrenException이 발생한다")
        void deleteRegion_fail_whenHasChildren() {
            // given
            UUID regionId = UUID.randomUUID();
            Long currentUserId = 1L;

            Region region = Region.create(
                    "1100000000",
                    "서울특별시",
                    null,
                    1,
                    true
            );

            Region child = Region.create(
                    "1111000000",
                    "종로구",
                    regionId,
                    2,
                    true
            );

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.of(region));
            given(regionRepository.findByParentId(regionId)).willReturn(List.of(child));

            // when & then
            assertThatThrownBy(() -> regionService.deleteRegion(regionId, currentUserId))
                    .isInstanceOf(RegionHasChildrenException.class)
                    .hasMessageContaining("하위 지역");

            then(regionRepository).should().findByRegionId(regionId);
            then(regionRepository).should().findByParentId(regionId);
        }

        @Test
        @DisplayName("삭제 대상 지역이 없으면 RegionNotFoundException이 발생한다")
        void deleteRegion_fail_whenRegionNotFound() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionRepository.findByRegionId(regionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.deleteRegion(regionId, 1L))
                    .isInstanceOf(RegionNotFoundException.class)
                    .hasMessageContaining("지역");

            then(regionRepository).should().findByRegionId(regionId);
        }
    }
}
