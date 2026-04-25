package com.sparta.delivery.review.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.sparta.delivery.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    INVALID_RATING(HttpStatus.BAD_REQUEST, "REVIEW-001", "평점은 1~5 사이 정수만 가능합니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "REVIEW-002", "리뷰글은 500자 이하로 작성해야합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
