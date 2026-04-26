package com.sparta.delivery.ai.presentation;

import com.sparta.delivery.ai.application.LlmService;
import com.sparta.delivery.ai.presentation.dto.request.LlmCreateRequest;
import com.sparta.delivery.ai.presentation.dto.request.LlmUpdateRequest;
import com.sparta.delivery.ai.presentation.dto.response.LlmResponse;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
@RestController
@Tag(name = "Llm", description = "LLM API")
@RequestMapping("/api/v1/llms")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @Operation(summary = "LLM 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<LlmResponse>> createLlm(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid LlmCreateRequest request
    ) {
        LlmResponse response = llmService.create(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(summary = "LLM 단건 조회")
    @GetMapping("/{llmId}")
    public ResponseEntity<ApiResponse<LlmResponse>> getLlm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID llmId
            ) {
        LlmResponse response = llmService.getLlm(
                UserRole.valueOf(principal.getRole()),
                llmId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "LLM 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LlmResponse>>> getLlms(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<LlmResponse> response = llmService.getLlms(
                UserRole.valueOf(principal.getRole())
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "LLM 이름 변경")
    @PatchMapping("/{llmId}")
    public ResponseEntity<ApiResponse<LlmResponse>> changeLlmName(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID llmId,
            @RequestBody @Valid LlmUpdateRequest request
            ) {
        LlmResponse response = llmService.changeLlmName(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                llmId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "LLM 삭제")
    @DeleteMapping("/{llmId}")
    public ResponseEntity<ApiResponse<LlmResponse>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID llmId
    ) {
        LlmResponse response = llmService.delete(
                principal.getId(),
                UserRole.valueOf(principal.getRole()),
                llmId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
