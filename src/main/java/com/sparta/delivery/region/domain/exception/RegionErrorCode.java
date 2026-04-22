package com.sparta.delivery.region.domain.exception;


import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION-001", "지역을 찾을 수 없습니다."),
    DUPLICATE_REGION_CODE(HttpStatus.CONFLICT, "REGION-002", "이미 사용 중인 지역 코드입니다."),
    INVALID_PARENT_REGION(HttpStatus.BAD_REQUEST, "REGION-003", "유효하지 않은 상위 지역입니다."),
    INVALID_REGION_DEPTH(HttpStatus.BAD_REQUEST, "REGION-004", "지역 depth 값이 올바르지 않습니다."),
    REGION_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "REGION-005", "하위 지역이 존재하여 삭제할 수 없습니다."),
    INVALID_REGION_CODE(HttpStatus.BAD_REQUEST, "REGION-006", "지역 코드는 10자리 숫자여야 합니다."),
    INVALID_REGION_NAME(HttpStatus.BAD_REQUEST, "REGION-007", "지역명은 비어 있을 수 없습니다."),
    INVALID_REGION_ACTIVE_STATUS(HttpStatus.BAD_REQUEST, "REGION-008", "지역 활성 여부는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
