package com.sparta.delivery.region.application;

import com.sparta.delivery.region.domain.entity.Region;
import com.sparta.delivery.region.domain.exception.DuplicateRegionCodeException;
import com.sparta.delivery.region.domain.exception.InvalidParentRegionException;
import com.sparta.delivery.region.domain.exception.InvalidRegionDepthException;
import com.sparta.delivery.region.domain.exception.RegionHasChildrenException;
import com.sparta.delivery.region.domain.exception.RegionNotFoundException;
import com.sparta.delivery.region.domain.repository.RegionRepository;
import com.sparta.delivery.region.presentation.dto.RegionCreateRequest;
import com.sparta.delivery.region.presentation.dto.RegionResponse;
import com.sparta.delivery.region.presentation.dto.RegionUpdateRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    /** 지역 생성 */
    @Transactional
    public RegionResponse createRegion(RegionCreateRequest request) {
        validateDuplicateRegionCode(request.regionCode());
        validateParentAndDepth(request.parentId(), request.depth());

        Region region = Region.create(
                request.regionCode(),
                request.regionName(),
                request.parentId(),
                request.depth(),
                request.isActive()
        );

        return RegionResponse.from(regionRepository.save(region));
    }

    /** 지역 단건 조회 */
    public RegionResponse getRegion(UUID regionId) {
        Region region = getRegionOrThrow(regionId);
        return RegionResponse.from(region);
    }

    /** 최상위 지역 목록 조회 */
    public List<RegionResponse> getRootRegions() {
        return regionRepository.findByParentIdIsNull().stream()
                .map(RegionResponse::from)
                .toList();
    }

    /** 특정 지역의 하위 지역 목록 조회 */
    public List<RegionResponse> getChildRegions(UUID parentId) {
        return regionRepository.findByParentId(parentId).stream()
                .map(RegionResponse::from)
                .toList();
    }

    /** 지역 정보 수정 */
    @Transactional
    public RegionResponse updateRegion(UUID regionId, RegionUpdateRequest request) {
        Region region = getRegionOrThrow(regionId);

        // 자기 자신을 부모로 지정할 수 없음
        if (request.parentId() != null && request.parentId().equals(regionId)) {
            throw new InvalidParentRegionException();
        }

        validateParentAndDepth(request.parentId(), request.depth());

        region.update(
                request.regionName(),
                request.parentId(),
                request.depth(),
                request.isActive()
        );

        return RegionResponse.from(region);
    }

    /** 하위 지역이 없을 때만 삭제 */
    @Transactional
    public void deleteRegion(UUID regionId, Long currentUserId) {
        Region region = getRegionOrThrow(regionId);

        if (!regionRepository.findByParentId(regionId).isEmpty()) {
            throw new RegionHasChildrenException();
        }

        region.softDelete(currentUserId);
    }

    /** 지역 조회, 없으면 예외 */
    private Region getRegionOrThrow(UUID regionId) {
        return regionRepository.findByRegionId(regionId)
                .orElseThrow(RegionNotFoundException::new);
    }

    /** 지역 코드 중복 검사 */
    private void validateDuplicateRegionCode(String regionCode) {
        if (regionRepository.existsByRegionCode(regionCode)) {
            throw new DuplicateRegionCodeException();
        }
    }

    /** 부모 지역과 depth 규칙 검사 */
    private void validateParentAndDepth(UUID parentId, Integer depth) {
        if (parentId == null) {
            if (depth != 1) {
                throw new InvalidRegionDepthException();
            }
            return;
        }

        Region parent = regionRepository.findByRegionId(parentId)
                .orElseThrow(InvalidParentRegionException::new);

        if (depth != parent.getDepth() + 1) {
            throw new InvalidRegionDepthException();
        }
    }
}