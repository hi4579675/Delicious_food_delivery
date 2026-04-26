package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements ErrorCode {
    INVALID_REGION_ID(HttpStatus.BAD_REQUEST, "STORE-001", "지역 ID는 필수입니다."),
    INVALID_CATEGORY_ID(HttpStatus.BAD_REQUEST, "STORE-002", "카테고리 ID는 필수입니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "STORE-003", "사용자 ID가 올바르지 않습니다."),
    INVALID_STORE_NAME(HttpStatus.BAD_REQUEST, "STORE-004", "가게명은 비어 있을 수 없습니다."),
    INVALID_STORE_ADDRESS(HttpStatus.BAD_REQUEST, "STORE-005", "가게 주소는 비어 있을 수 없습니다."),
    INVALID_MIN_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "STORE-006", "최소 주문 금액은 0 이상이어야 합니다."),
    INVALID_STORE_OPEN_STATUS(HttpStatus.BAD_REQUEST, "STORE-007", "영업 상태는 필수입니다."),
    INVALID_STORE_ACTIVE_STATUS(HttpStatus.BAD_REQUEST, "STORE-008", "활성 상태는 필수입니다."),
    INVALID_REVIEW_COUNT(HttpStatus.BAD_REQUEST, "STORE-009", "리뷰 수는 0 이상이어야 합니다."),
    INVALID_STORE_REGION_DEPTH(HttpStatus.BAD_REQUEST, "STORE-010", "가게 지역은 depth 3 지역만 선택할 수 있습니다."),
    INVALID_CATEGORY_NAME(HttpStatus.BAD_REQUEST, "STORE-011", "카테고리명은 비어 있을 수 없습니다."),
    INVALID_CATEGORY_SORT_ORDER(HttpStatus.BAD_REQUEST, "STORE-012", "카테고리 정렬 순서는 0 이상이어야 합니다."),
    INVALID_CATEGORY_ACTIVE_STATUS(HttpStatus.BAD_REQUEST, "STORE-013", "카테고리 활성 상태는 필수입니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-014", "가게를 찾을 수 없습니다."),
    STORE_FORBIDDEN(HttpStatus.FORBIDDEN, "STORE-015", "해당 가게에 대한 권한이 없습니다."),
    STORE_REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-016", "가게 지역을 찾을 수 없습니다."),
    STORE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-017", "가게 카테고리를 찾을 수 없습니다."),
    INACTIVE_STORE_REGION(HttpStatus.BAD_REQUEST, "STORE-018", "비활성 지역에는 가게를 등록할 수 없습니다."),
    INACTIVE_STORE_CATEGORY(HttpStatus.BAD_REQUEST, "STORE-019", "비활성 카테고리에는 가게를 등록할 수 없습니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "STORE-020", "이미 사용 중인 카테고리명입니다."),
    DUPLICATE_CATEGORY_SORT_ORDER(HttpStatus.CONFLICT, "STORE-021", "이미 사용 중인 카테고리 정렬 순서입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
