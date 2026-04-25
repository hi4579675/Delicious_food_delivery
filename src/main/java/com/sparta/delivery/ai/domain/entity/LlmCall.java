package com.sparta.delivery.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_llm_calls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "call_id")
    private UUID callId;

    @Column(name = "llm_id", nullable = false)
    private UUID llmId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    private String inputSnapshot;

    @Column(name = "provider_status_code", length = 50)
    private String providerStatusCode;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "generated_text", columnDefinition = "TEXT")
    private String generatedText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder(access = AccessLevel.PRIVATE)
    private LlmCall(
            UUID llmId,
            UUID productId,
            String inputSnapshot,
            String providerStatusCode,
            String rawResponse,
            String generatedText,
            LocalDateTime createdAt,
            Long createdBy
    ) {
        this.llmId = llmId;
        this.productId = productId;
        this.inputSnapshot = inputSnapshot;
        this.providerStatusCode = providerStatusCode;
        this.rawResponse = rawResponse;
        this.generatedText = generatedText;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static LlmCall create(
            UUID llmId,
            UUID productId,
            String inputSnapshot,
            String providerStatusCode,
            String rawResponse,
            String generatedText,
            Long createdBy
    ) {
        return LlmCall.builder()
                .llmId(llmId)
                .productId(productId)
                .inputSnapshot(inputSnapshot)
                .providerStatusCode(providerStatusCode)
                .rawResponse(rawResponse)
                .generatedText(generatedText)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
    }

}
