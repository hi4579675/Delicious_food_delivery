package com.sparta.delivery.review.presentation;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.common.response.PageResponse;
import com.sparta.delivery.review.application.service.ReviewService;
import com.sparta.delivery.review.presentation.dto.request.ReviewCreateRequest;
import com.sparta.delivery.review.presentation.dto.request.ReviewUpdateRequest;
import com.sparta.delivery.review.presentation.dto.response.ReviewResponse;
import com.sparta.delivery.user.domain.entity.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Review", description = "리뷰 API")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 생성")
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                reviewService.create(principal.getId(), UserRole.valueOf(principal.getRole()), request)
        ));
    }

    @Operation(summary = "리뷰 단건 조회")
    @GetMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> getByReviewId(
            @PathVariable UUID reviewId
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getByReviewId(reviewId)));
    }

    @Operation(summary = "가게 리뷰 목록 조회")
    @GetMapping("/stores/{storeId}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReviews(storeId, page, size, sortBy, direction)
        ));
    }

    @Operation(summary = "리뷰 수정")
    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.update(principal.getId(), reviewId, UserRole.valueOf(principal.getRole()), request)
        ));
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId
    ) {
        reviewService.delete(principal.getId(), reviewId, UserRole.valueOf(principal.getRole()));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
