package com.sparta.delivery.ai.presentation;

import com.sparta.delivery.ai.application.LlmCallService;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallListResponse;
import com.sparta.delivery.ai.presentation.dto.response.LlmCallResponse;
import com.sparta.delivery.common.config.security.UserPrincipal;
import com.sparta.delivery.common.response.ApiResponse;
import com.sparta.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "LlmCall", description = "LLM 호출 로그 API")
@RequestMapping("/api/v1/llm-calls")
@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
@RequiredArgsConstructor
public class LlmCallController {

    private final LlmCallService llmCallService;

    @Operation(summary = "LLM 호출 로그 단건 조회")
    @GetMapping("/{callId}")
    public ResponseEntity<ApiResponse<LlmCallResponse>> getLlmCall(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID callId
            ) {
        LlmCallResponse response = llmCallService.getLlmCall(
                UserRole.valueOf(principal.getRole()),
                callId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "LLM 호출 로그 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LlmCallListResponse>>> getLlmCalls(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<LlmCallListResponse> response = llmCallService.getLlmCalls(UserRole.valueOf(principal.getRole()));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
