package com.sparta.delivery.review.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.sparta.delivery.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    INVALID_RATING(HttpStatus.BAD_REQUEST, "REVIEW-001", "평점은 1~5 사이 정수만 가능합니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "REVIEW-002", "리뷰글은 500자 이하로 작성해야합니다."),
    INVALID_ORDER_ID(HttpStatus.BAD_REQUEST, "REVIEW-003", "유효하지 않은 주문 ID입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "REVIEW-004", "주문 완료 상태에서만 리뷰를 작성할 수 있습니다."),

    DUPLICATE_REVIEW_ORDER(HttpStatus.CONFLICT, "REVIEW-005", "이미 해당 주문에 대한 리뷰가 존재하거나 삭제되었습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW-006", "리뷰를 작성할 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW-007", "리뷰 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
